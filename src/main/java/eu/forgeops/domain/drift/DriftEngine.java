package eu.forgeops.domain.drift;

import com.jcraft.jsch.Session;
import eu.forgeops.domain.forge.Forge;
import eu.forgeops.domain.forge.ForgeVersion;
import eu.forgeops.domain.inventory.Node;
import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleRegistry;
import eu.forgeops.engine.parser.ForgeSpecDocument;
import eu.forgeops.engine.parser.ForgeSpecParser;
import eu.forgeops.engine.parser.ForgeTask;
import eu.forgeops.engine.ssh.SshConnectionManager;
import eu.forgeops.infra.persistence.DriftReportRepository;
import eu.forgeops.infra.persistence.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriftEngine {

    private final DriftReportRepository driftReportRepository;
    private final NodeRepository nodeRepository;
    private final SshConnectionManager sshConnectionManager;
    private final ModuleRegistry moduleRegistry;
    private final ForgeSpecParser forgeSpecParser;

    @Async
    public void checkDrift(DriftReport report, Forge forge, ForgeVersion version) {
        report.setStatus("running");
        report.setStartedAt(OffsetDateTime.now());
        driftReportRepository.save(report);

        try {
            ForgeSpecDocument spec = forgeSpecParser.parse(version.getContent());
            List<UUID> nodeIds = collectTargetNodeIds(forge.getId());
            List<Node> nodes = nodeRepository.findAllWithDetailsByIdIn(nodeIds);

            List<DriftItem> items = Collections.synchronizedList(new ArrayList<>());

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = nodes.stream()
                    .map(node -> executor.submit(() -> checkNodeDrift(report, node, spec, items)))
                    .toList();
                for (var f : futures) {
                    try { f.get(); } catch (Exception e) {
                        log.warn("Drift check failed for a node: {}", e.getMessage());
                    }
                }
            }

            report.getItems().addAll(items);
            report.setDriftCount((int) items.stream().filter(i -> "drifted".equals(i.getStatus())).count());
            report.setStatus("completed");
        } catch (Exception e) {
            log.error("Drift check failed", e);
            report.setStatus("failed");
        }

        report.setCompletedAt(OffsetDateTime.now());
        driftReportRepository.save(report);
    }

    private void checkNodeDrift(DriftReport report, Node node, ForgeSpecDocument spec, List<DriftItem> out) {
        Session session = null;
        try {
            session = sshConnectionManager.openSession(node, buildVars(node, spec));
        } catch (Exception e) {
            log.warn("Cannot SSH to node {} for drift check: {}", node.getName(), e.getMessage());
            out.add(DriftItem.builder()
                .report(report).nodeId(node.getId())
                .taskName("connection").status("error")
                .actualState("SSH failed: " + e.getMessage())
                .build());
            return;
        }

        for (ForgeTask task : spec.getTasks()) {
            var module = moduleRegistry.get(task.getModule());
            if (module == null) continue;

            ModuleContext ctx = ModuleContext.builder()
                .nodeId(node.getId()).nodeName(node.getName())
                .hostname(node.getHostname()).port(node.getPort())
                .osType(node.getOsType()).sshSession(session)
                .nodeVariables(buildVars(node, spec))
                .build();

            try {
                Map<String, Object> actual = module.checkState(ctx, task.getParams() != null ? task.getParams() : Map.of());
                if (!actual.isEmpty()) {
                    out.add(DriftItem.builder()
                        .report(report).nodeId(node.getId())
                        .taskName(task.getName() != null ? task.getName() : task.getModule())
                        .status(inferDrift(task.getParams(), actual) ? "drifted" : "ok")
                        .expectedState(task.getParams() != null ? task.getParams().toString() : "")
                        .actualState(actual.toString())
                        .build());
                }
            } catch (Exception e) {
                log.debug("Module checkState failed for task {}: {}", task.getName(), e.getMessage());
            }
        }

        sshConnectionManager.closeSession(session);
    }

    private boolean inferDrift(Map<String, Object> expected, Map<String, Object> actual) {
        if (expected == null || actual == null) return false;
        // Check "state" key
        String expState = (String) expected.get("state");
        if (expState != null) {
            Object actState = actual.get("state");
            return !expState.equals(actState);
        }
        // Check "installed" key for packages
        if (expected.containsKey("state")) {
            boolean wantInstalled = !"absent".equals(expected.get("state"));
            Object installed = actual.get("installed");
            if (installed instanceof Boolean b) {
                return wantInstalled != b;
            }
        }
        return false;
    }

    private Map<String, Object> buildVars(Node node, ForgeSpecDocument spec) {
        Map<String, Object> vars = new HashMap<>();
        if (spec.getVars() != null) vars.putAll(spec.getVars());
        node.getVariables().forEach(v -> vars.put(v.getKey(), v.getValue()));
        return vars;
    }

    private List<UUID> collectTargetNodeIds(UUID forgeId) {
        return nodeRepository.findNodeIdsByForgeId(forgeId);
    }
}
