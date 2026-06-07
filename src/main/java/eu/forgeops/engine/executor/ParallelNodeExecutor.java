package eu.forgeops.engine.executor;

import com.jcraft.jsch.Session;
import eu.forgeops.domain.inventory.Group;
import eu.forgeops.domain.inventory.Node;
import eu.forgeops.domain.inventory.NodeVariable;
import eu.forgeops.domain.run.Run;
import eu.forgeops.domain.run.RunLog;
import eu.forgeops.domain.run.RunTask;
import eu.forgeops.engine.parser.ForgeSpecDocument;
import eu.forgeops.engine.parser.ForgeTask;
import eu.forgeops.engine.ssh.SshConnectionManager;
import eu.forgeops.infra.persistence.NodeRepository;
import eu.forgeops.infra.persistence.RunLogRepository;
import eu.forgeops.infra.persistence.RunRepository;
import eu.forgeops.infra.persistence.RunTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParallelNodeExecutor {

    private static final String CANCEL_KEY_PATTERN = "forgeops:runs:%s:cancel";

    private final NodeRepository nodeRepository;
    private final RunRepository runRepository;
    private final RunTaskRepository runTaskRepository;
    private final RunLogRepository runLogRepository;
    private final SshConnectionManager sshConnectionManager;
    private final ModuleRegistry moduleRegistry;
    private final OutputHandler outputHandler;
    private final StringRedisTemplate redisTemplate;

    public void execute(Run run, ForgeSpecDocument spec) {
        run.setStatus("running");
        run.setStartedAt(OffsetDateTime.now());
        runRepository.save(run);

        List<UUID> nodeIds = run.getTargetNodes();
        // Eagerly fetch variables and groups to avoid LazyInitializationException in virtual threads
        List<Node> nodes = nodeRepository.findAllWithDetailsByIdIn(nodeIds);

        int ok = 0, changed = 0, failed = 0, skipped = 0;
        Map<UUID, String> nodeResults = new LinkedHashMap<>();

        // Java 21 virtual threads — one per node
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<NodeResult>> futures = nodes.stream()
                .map(node -> executor.submit(() -> executeOnNode(run, node, spec)))
                .toList();

            for (Future<NodeResult> future : futures) {
                try {
                    NodeResult result = future.get(30, TimeUnit.MINUTES);
                    nodeResults.put(result.nodeId(), result.status());
                    switch (result.status()) {
                        case "ok"      -> ok++;
                        case "changed" -> { ok++; changed++; }
                        case "failed"  -> failed++;
                        case "skipped" -> skipped++;
                        default        -> ok++;
                    }
                } catch (Exception e) {
                    log.error("Node execution failed", e);
                    failed++;
                }
            }
        }

        boolean cancelled = Boolean.TRUE.equals(redisTemplate.hasKey(CANCEL_KEY_PATTERN.formatted(run.getId())));
        run.setStatus(cancelled ? "cancelled" : (failed > 0 ? "failed" : "success"));
        run.setCompletedAt(OffsetDateTime.now());
        run.setSummary(Map.of(
            "total", nodes.size(),
            "ok", ok,
            "changed", changed,
            "failed", failed,
            "skipped", skipped
        ));
        runRepository.save(run);

        // clean up cancel flag
        redisTemplate.delete(CANCEL_KEY_PATTERN.formatted(run.getId()));
    }

    private NodeResult executeOnNode(Run run, Node node, ForgeSpecDocument spec) {
        String runStatus = "ok";
        Session session = null;
        Map<String, Object> resolvedVars = resolveVars(node, spec);

        try {
            session = sshConnectionManager.openSession(node, resolvedVars);
        } catch (Exception e) {
            log.error("Failed to SSH connect to node {}: {}", node.getName(), e.getMessage());
            return new NodeResult(node.getId(), "failed");
        }

        List<ForgeTask> tasks = spec.getTasks() != null ? spec.getTasks() : List.of();
        Map<String, Object> registers = new HashMap<>();
        List<String> triggeredHandlers = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            if (isCancelled(run.getId())) break;

            ForgeTask task = tasks.get(i);
            RunTask runTask = RunTask.builder()
                .run(run)
                .nodeId(node.getId())
                .taskIndex(i)
                .taskName(task.getName())
                .module(task.getModule())
                .status("running")
                .startedAt(OffsetDateTime.now())
                .build();
            runTask = runTaskRepository.save(runTask);

            if (task.getWhen() != null && !evaluateWhen(task.getWhen(), resolvedVars, registers)) {
                runTask.setStatus("skipped");
                runTask.setCompletedAt(OffsetDateTime.now());
                runTaskRepository.save(runTask);
                continue;
            }

            ForgeModule module = moduleRegistry.get(task.getModule());
            if (module == null) {
                persistLog(runTask, 0, "error", "Unknown module: " + task.getModule());
                runTask.setStatus("failed");
                runTask.setExitCode(1);
                runTask.setCompletedAt(OffsetDateTime.now());
                runTaskRepository.save(runTask);
                if (!task.isIgnoreErrors()) {
                    runStatus = "failed";
                    break;
                }
                continue;
            }

            // Build context
            final UUID taskId = runTask.getId();
            final RunTask finalTask = runTask;
            ModuleContext ctx = ModuleContext.builder()
                .nodeId(node.getId())
                .nodeName(node.getName())
                .hostname(node.getHostname())
                .port(node.getPort())
                .osType(node.getOsType())
                .sshSession(session)
                .nodeVariables(resolvedVars)
                .runId(run.getId())
                .taskId(taskId)
                .forgeName(spec.getName())
                .outputHandler((rId, tId, nId, nName, lineNo, level, msg) -> {
                    outputHandler.emit(rId, tId, nId, nName, lineNo, level, msg);
                    persistLog(finalTask, lineNo, level, msg);
                })
                .build();

            // Merge task-level vars
            if (task.getVars() != null) {
                ctx.getNodeVariables().putAll(task.getVars());
            }

            // Resolve params against variables
            Map<String, Object> resolvedParams = resolveParams(task.getParams(), ctx.getVars(), registers);

            ModuleResult result = module.execute(ctx, resolvedParams);

            boolean resultFailed = result.getStatus() == ModuleResult.Status.FAILED;
            boolean resultChanged = result.getStatus() == ModuleResult.Status.CHANGED;

            if (task.getRegister() != null) {
                registers.put(task.getRegister(), Map.of(
                    "rc", result.getExitCode(),
                    "changed", resultChanged,
                    "failed", resultFailed,
                    "stdout", result.getMessage() != null ? result.getMessage() : ""
                ));
            }

            runTask.setStatus(resultFailed ? "failed" : resultChanged ? "changed" : "ok");
            runTask.setExitCode(result.getExitCode());
            runTask.setCompletedAt(OffsetDateTime.now());
            runTaskRepository.save(runTask);

            if (resultFailed) {
                if (!task.isIgnoreErrors()) {
                    runStatus = "failed";
                    break;
                }
            } else if (resultChanged) {
                runStatus = "changed";
                if (task.getNotify() != null) {
                    triggeredHandlers.addAll(task.getNotify());
                }
            }
        }

        // Run triggered handlers
        if (!triggeredHandlers.isEmpty() && spec.getHandlers() != null) {
            for (ForgeTask handler : spec.getHandlers()) {
                if (triggeredHandlers.contains(handler.getName())) {
                    runHandler(run, node, handler, resolvedVars, session);
                }
            }
        }

        sshConnectionManager.closeSession(session);
        return new NodeResult(node.getId(), runStatus);
    }

    private void runHandler(Run run, Node node, ForgeTask handler, Map<String, Object> vars, Session session) {
        ForgeModule module = moduleRegistry.get(handler.getModule());
        if (module == null) return;
        ModuleContext ctx = ModuleContext.builder()
            .nodeId(node.getId()).nodeName(node.getName())
            .hostname(node.getHostname()).port(node.getPort())
            .osType(node.getOsType()).sshSession(session)
            .nodeVariables(vars).runId(run.getId())
            .outputHandler(outputHandler)
            .build();
        module.execute(ctx, handler.getParams() != null ? handler.getParams() : Map.of());
    }

    private Map<String, Object> resolveVars(Node node, ForgeSpecDocument spec) {
        Map<String, Object> vars = new HashMap<>();
        // Group vars first (lower priority)
        for (Group g : node.getGroups()) {
            g.getVariables().forEach(v -> vars.put(v.getKey(), v.getValue()));
        }
        // Spec-level vars
        if (spec.getVars() != null) {
            vars.putAll(spec.getVars());
        }
        // Node vars (highest priority)
        for (NodeVariable v : node.getVariables()) {
            vars.put(v.getKey(), v.getValue());
        }
        return vars;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveParams(Map<String, Object> params, Map<String, Object> vars,
                                               Map<String, Object> registers) {
        if (params == null) return Map.of();
        Map<String, Object> resolved = new HashMap<>(params.size());
        for (Map.Entry<String, Object> e : params.entrySet()) {
            Object val = e.getValue();
            if (val instanceof String s) {
                val = interpolate(s, vars, registers);
            }
            resolved.put(e.getKey(), val);
        }
        return resolved;
    }

    private String interpolate(String template, Map<String, Object> vars, Map<String, Object> registers) {
        String result = template;
        // Replace {{ var_name }}
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            result = result.replace("{{ " + e.getKey() + " }}", String.valueOf(e.getValue()))
                           .replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
        }
        return result;
    }

    private boolean evaluateWhen(String when, Map<String, Object> vars, Map<String, Object> registers) {
        // Simple evaluation: check truthy variable or register result
        if (when == null || when.isBlank()) return true;
        // Direct variable lookup
        String key = when.trim();
        if (vars.containsKey(key)) {
            Object val = vars.get(key);
            if (val instanceof Boolean b) return b;
            if (val instanceof String s) return !s.isBlank() && !"false".equalsIgnoreCase(s);
            return val != null;
        }
        // Check negation
        if (key.startsWith("not ")) {
            return !evaluateWhen(key.substring(4), vars, registers);
        }
        return true;
    }

    private void persistLog(RunTask runTask, int lineNo, String level, String message) {
        try {
            RunLog log = RunLog.builder()
                .runTask(runTask)
                .lineNo(lineNo)
                .level(level)
                .message(message)
                .build();
            runLogRepository.save(log);
        } catch (Exception e) {
            // Don't fail the run because of a logging error
        }
    }

    private boolean isCancelled(UUID runId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CANCEL_KEY_PATTERN.formatted(runId)));
    }

    record NodeResult(UUID nodeId, String status) {}
}
