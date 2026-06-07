# Phase 4 — Run Engine

## Goal
Implement the full run execution engine: SSH execution, module dispatch, parallel node execution, real-time output streaming via Redis + WebSocket.

## Tasks

### 4.1 — JPA Entities
Create `Run`, `RunTask`, `RunLog` entities.
Create `RunRepository`, `RunTaskRepository`, `RunLogRepository`.

### 4.2 — SSH Connection Manager
`SshConnectionManager`:
- Opens JSch SSH sessions per node (using credential from Vault)
- Reuses sessions within a run
- Cleans up on completion/error
- Implements connect timeout from config

### 4.3 — Module Implementations
Create `ForgeModule` interface (as designed in `docs/07_run_engine.md`).
Implement all 9 built-in modules from `docs/04_forgespec_dsl.md`:
`PackageModule`, `ServiceModule`, `FileModule`, `TemplateModule`, `CommandModule`, `UserModule`, `CopyModule`, `CronModule`, `MountModule`, `WaitForModule`.
Each module generates OS-appropriate shell commands.
Each module implements `checkState()` for drift detection.

### 4.4 — Output Streaming
`RedisOutputPublisher` — publishes each output line to Redis channel `forgeops:runs:{runId}:output`.
`RunOutputWebSocketRelay` — Spring Redis MessageListener that subscribes and forwards to STOMP topic `/topic/runs/{runId}/output`.

### 4.5 — Parallel Node Executor
`ParallelNodeExecutor` (as designed in `docs/07_run_engine.md`):
- One virtual thread (Java 21) per node
- Sequential task execution per node
- Handler collection and post-run execution
- Cancellation flag check between tasks

### 4.6 — Run Service
`RunService`:
- `createRun` — resolves targets, creates DB records, publishes ApplicationEvent
- `executeRun` — called async by event handler, drives ParallelNodeExecutor
- `cancelRun` — sets Redis cancellation flag
- `getRun`, `listRuns` (paginated, filterable)

### 4.7 — WinRM (Stub)
Implement a basic WinRM stub (PowerShell execution) — can be a simple HTTP client to WinRM endpoint.
Mark as `@ConditionalOnProperty(name = "forgeops.engine.winrm.enabled")`.

### 4.8 — WebSocket Config
Configure Spring STOMP WebSocket at `/ws`.
Configure Redis message listeners for all run output channels.

### 4.9 — Run REST Controllers
`RunController` — all endpoints from `docs/05_api_spec.md` under `/runs`.

## Acceptance Criteria
- [ ] `POST /api/v1/runs` against a real (test) node with an echo command produces real output
- [ ] WebSocket client receives streamed output lines in real time
- [ ] Run summary counts (ok/changed/failed) are correct
- [ ] `DELETE /api/v1/runs/{id}` cancels a running run
- [ ] All 9 modules build correct shell commands for Linux

---

