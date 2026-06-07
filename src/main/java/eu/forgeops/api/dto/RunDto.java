package eu.forgeops.api.dto;

import eu.forgeops.domain.run.Run;
import eu.forgeops.domain.run.RunTask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RunDto {

    public record RunResponse(
        UUID id,
        UUID forgeVersionId,
        String forgeName,
        int forgeVersion,
        UUID triggeredBy,
        String triggerType,
        String status,
        List<UUID> targetNodes,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Map<String, Object> summary,
        OffsetDateTime createdAt
    ) {
        public static RunResponse from(Run run) {
            var fv = run.getForgeVersion();
            return new RunResponse(
                run.getId(),
                fv != null ? fv.getId() : null,
                fv != null && fv.getForge() != null ? fv.getForge().getName() : null,
                fv != null ? fv.getVersion() : 0,
                run.getTriggeredBy(),
                run.getTriggerType(),
                run.getStatus(),
                run.getTargetNodes(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getSummary(),
                run.getCreatedAt()
            );
        }
    }

    public record CreateRunRequest(
        UUID forgeVersionId,
        List<UUID> targetNodeIds
    ) {}

    public record RunTaskResponse(
        UUID id,
        UUID nodeId,
        int taskIndex,
        String taskName,
        String module,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer exitCode
    ) {
        public static RunTaskResponse from(RunTask t) {
            return new RunTaskResponse(
                t.getId(), t.getNodeId(), t.getTaskIndex(), t.getTaskName(),
                t.getModule(), t.getStatus(), t.getStartedAt(), t.getCompletedAt(), t.getExitCode()
            );
        }
    }
}
