package eu.forgeops.api.controller;

import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.api.dto.RunDto;
import eu.forgeops.api.dto.RunDto.RunResponse;
import eu.forgeops.api.dto.RunDto.RunTaskResponse;
import eu.forgeops.domain.run.Run;
import eu.forgeops.domain.run.RunService;
import eu.forgeops.infra.persistence.RunLogRepository;
import eu.forgeops.infra.persistence.RunTaskRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RunController {

    private final RunService runService;
    private final RunTaskRepository runTaskRepository;
    private final RunLogRepository runLogRepository;

    @PostMapping("/forges/{forgeId}/runs")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<RunResponse> createRun(
        @PathVariable UUID forgeId,
        @Valid @RequestBody RunDto.CreateRunRequest req,
        @AuthenticationPrincipal UserDetails principal
    ) {
        UUID userId = extractUserId(principal);
        Run run = runService.createRun(req.forgeVersionId(), req.targetNodeIds(), userId, "manual");
        runService.executeRunAsync(run.getId());
        return ResponseEntity
            .created(URI.create("/api/v1/runs/" + run.getId()))
            .body(RunResponse.from(run));
    }

    @GetMapping("/runs/{runId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public RunResponse getRun(@PathVariable UUID runId) {
        return RunResponse.from(runService.getById(runId));
    }

    @GetMapping("/forges/{forgeId}/runs")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public PageResponse<RunResponse> listRuns(
        @PathVariable UUID forgeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(runService.listByForge(forgeId, pageable).map(RunResponse::from));
    }

    @PostMapping("/runs/{runId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> cancelRun(@PathVariable UUID runId) {
        runService.cancelRun(runId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/runs/{runId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public java.util.List<RunTaskResponse> getRunTasks(@PathVariable UUID runId) {
        return runTaskRepository.findByRunIdOrderByStartedAtAsc(runId)
            .stream().map(RunTaskResponse::from).toList();
    }

    @GetMapping("/runs/{runId}/logs")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public java.util.List<?> getRunLogs(@PathVariable UUID runId) {
        return runLogRepository.findByRunId(runId).stream().map(log -> new java.util.LinkedHashMap<String, Object>() {{
            put("lineNo", log.getLineNo());
            put("level", log.getLevel());
            put("message", log.getMessage());
            put("loggedAt", log.getLoggedAt());
            put("taskId", log.getRunTask() != null ? log.getRunTask().getId() : null);
        }}).toList();
    }

    private UUID extractUserId(UserDetails principal) {
        // The username is stored as UUID in our setup (loaded by ForgeUserDetailsService)
        try {
            return UUID.fromString(principal.getUsername());
        } catch (Exception e) {
            return null;
        }
    }
}
