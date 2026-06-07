# ForgeOps — Run Engine

## Overview

The Run Engine is responsible for:
1. Parsing and compiling a ForgeSpec into an execution plan
2. Establishing connections to target nodes (SSH / WinRM)
3. Executing tasks in the correct order with parallelism across nodes
4. Streaming output in real time to Redis PubSub
5. Tracking task state and persisting results to PostgreSQL
6. Enforcing timeouts, error handling, and cancellation

---

## Execution Flow

```
POST /runs
    │
    ▼
RunService.createRun()
    │  - Validates forge version
    │  - Resolves target nodes (groups → node list)
    │  - Creates Run record (status: PENDING)
    │  - Creates RunTask records for each (node × task) pair
    │  - Publishes to RunExecutor via Spring ApplicationEvent
    │
    ▼
RunExecutor.execute(runId)   [runs in @Async thread pool]
    │
    ▼
ForgeCompiler.compile(forgeVersionContent)
    │  - Parses YAML into ForgeSpec object graph
    │  - Resolves variables (node vars → group vars → forge vars → builtins)
    │  - Evaluates 'when' conditions per node
    │  - Expands 'loop' into individual task instances
    │  - Returns ExecutionPlan: { tasks[], nodeTargets[] }
    │
    ▼
ConnectionPool.acquire(nodeId)
    │  - For Linux nodes: SSH via JSch, connection reuse per run
    │  - For Windows nodes: WinRM HTTP/HTTPS client
    │  - Decrypts credential from Vault
    │
    ▼
ParallelNodeExecutor.run(ExecutionPlan)
    │  - One CompletableFuture per node (up to forkFactor threads)
    │  - Within each node: tasks run SEQUENTIALLY (order matters)
    │  - Handler tasks collected and run after all regular tasks
    │
    ▼
ModuleExecutor.execute(task, connection)
    │  - Dispatches to correct Module implementation
    │  - Module generates OS-specific shell commands
    │  - Commands sent over SSH/WinRM
    │  - Output streamed line by line via OutputStreamHandler
    │
    ▼
OutputStreamHandler
    │  - Receives each output line
    │  - Persists to run_logs table
    │  - Publishes to Redis channel: forgeops:runs:{runId}:output
    │  - Spring WS layer subscribes Redis → forwards to STOMP topic
    │
    ▼
RunFinalizer
    - Aggregates per-node task statuses
    - Computes run summary (ok/changed/failed/skipped counts)
    - Updates Run status (SUCCESS / FAILED / PARTIAL)
    - Closes all connections
    - Publishes final status to Redis → STOMP
```

---

## Parallelism Model

```
Run
├── Node: web-01 [CompletableFuture thread 1]
│   ├── Task 1 (sequential)
│   ├── Task 2 (sequential)
│   └── Task 3 (sequential)
├── Node: web-02 [CompletableFuture thread 2]
│   ├── Task 1
│   ├── Task 2
│   └── Task 3
└── Node: db-01 [CompletableFuture thread 3]
    └── ...

Max parallel nodes: configurable via FORGEOPS_ENGINE_FORK_FACTOR (default: 10)
Thread pool: VirtualThreadExecutor (Java 21 virtual threads)
```

---

## SSH Execution (JSch)

```java
// Connection setup
JSch jsch = new JSch();
jsch.addIdentity(sshKeyPath);        // from vault
Session session = jsch.getSession(user, host, port);
session.setConfig("StrictHostKeyChecking", "no");  // configurable
session.connect(connectTimeout);

// Command execution
ChannelExec channel = (ChannelExec) session.openChannel("exec");
channel.setCommand(buildCommand(task));
channel.setPty(false);

InputStream stdout = channel.getInputStream();
InputStream stderr = channel.getErrStream();
channel.connect();

// Stream output line by line
BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
String line;
int lineNo = 0;
while ((line = reader.readLine()) != null) {
    outputHandler.emit(lineNo++, "info", line);
}
int exitCode = channel.getExitStatus();
channel.disconnect();
```

---

## WinRM Execution

- Library: `io.cloudsoft.winrm4j` or custom HTTP client to WinRM endpoint
- Transport: HTTP (port 5985) or HTTPS (port 5986)
- Auth: NTLM or Kerberos (configurable per node credential)
- Commands executed as PowerShell scripts

---

## Module Implementation Pattern

Each module implements:

```java
public interface ForgeModule {
    String getName();              // e.g. "package"
    List<String> getSupportedOs(); // ["linux", "windows"] or subset
    
    ModuleResult execute(
        ModuleContext context,      // node info, variables, connection
        Map<String, Object> params, // task params from ForgeSpec
        OutputHandler output        // streaming output
    );
    
    // For policy/drift mode:
    Map<String, Object> checkState(ModuleContext context, Map<String, Object> params);
}
```

### Module: Package (Linux implementation)

```java
// Detects package manager
String pkgManager = detectPackageManager(context); // apt | yum | dnf

switch(pkgManager) {
    case "apt":
        cmd = switch(state) {
            case "present" -> "DEBIAN_FRONTEND=noninteractive apt-get install -y " + name;
            case "absent"  -> "apt-get remove -y " + name;
            case "latest"  -> "apt-get install -y --only-upgrade " + name;
        };
        break;
    case "yum":
        cmd = switch(state) {
            case "present" -> "yum install -y " + name;
            case "absent"  -> "yum remove -y " + name;
            case "latest"  -> "yum update -y " + name;
        };
        break;
}
```

---

## Drift Detection Engine

The drift checker runs:
1. On schedule (per forge_group_binding.check_interval)
2. On demand (POST /drift/reports)

For each node in the group:
1. Load the bound ForgeSpec policies block
2. For each policy, call `module.checkState(context, params)`
3. Compare returned state with expected state
4. If different → create DriftItem record
5. If `on_drift == "remediate"` → enqueue a remediation Run

The `checkState` method for each module runs a read-only probe:

```java
// Service module checkState example
String cmd = "systemctl is-active " + params.get("name");
String result = connection.execute(cmd).trim();
return Map.of("state", result.equals("active") ? "running" : "stopped");
```

---

## Cancellation

A run can be cancelled via `DELETE /runs/{id}`:
1. Sets a cancellation flag in Redis (`forgeops:runs:{runId}:cancel = 1`)
2. Each task executor checks this flag between tasks
3. On detection: cleanly closes SSH channels, marks remaining tasks as CANCELLED
4. Run status set to CANCELLED

---

## Error Handling

| Scenario | Behaviour |
|---|---|
| SSH connection fails | Task status = FAILED, error logged, continue other nodes |
| Task exit code != 0 | Task status = FAILED; if `ignore_errors: false` → stop this node's tasks |
| `ignore_errors: true` | Continue despite failure |
| Timeout exceeded | Kill channel, task status = FAILED with TIMEOUT message |
| Module not found | Run fails immediately before execution starts |
| Variable unresolvable | Warning logged, variable left as literal string |

---

## Configuration

```properties
# application.properties / env vars
FORGEOPS_ENGINE_FORK_FACTOR=10           # max parallel nodes per run
FORGEOPS_ENGINE_TASK_TIMEOUT=300         # default task timeout (seconds)
FORGEOPS_ENGINE_SSH_CONNECT_TIMEOUT=10   # SSH connect timeout (seconds)
FORGEOPS_ENGINE_SSH_STRICT_HOST_CHECK=false
FORGEOPS_ENGINE_WINRM_TIMEOUT=30
FORGEOPS_VAULT_ENCRYPTION_KEY=...        # AES-256 key for secret encryption
FORGEOPS_DRIFT_SCHEDULER_ENABLED=true
```
