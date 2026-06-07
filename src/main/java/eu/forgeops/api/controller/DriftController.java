package eu.forgeops.api.controller;

import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.domain.drift.DriftReport;
import eu.forgeops.domain.drift.DriftService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/forges/{forgeId}/drift")
@RequiredArgsConstructor
public class DriftController {

    private final DriftService driftService;

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Map<String, Object>> triggerDriftCheck(
        @PathVariable UUID forgeId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        DriftReport report = driftService.startDriftCheck(forgeId, userId);
        return ResponseEntity
            .created(URI.create("/api/v1/forges/" + forgeId + "/drift/" + report.getId()))
            .body(toMap(report));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public PageResponse<Map<String, Object>> listReports(
        @PathVariable UUID forgeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(driftService.listByForge(forgeId, pageable).map(this::toMap));
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Map<String, Object> getReport(
        @PathVariable UUID forgeId,
        @PathVariable UUID reportId
    ) {
        return toDetailMap(driftService.getById(reportId));
    }

    private Map<String, Object> toMap(DriftReport r) {
        return Map.of(
            "id", r.getId(),
            "forgeId", r.getForge().getId(),
            "status", r.getStatus(),
            "driftCount", r.getDriftCount(),
            "startedAt", r.getStartedAt() != null ? r.getStartedAt().toString() : "",
            "completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : "",
            "createdAt", r.getCreatedAt().toString()
        );
    }

    private Map<String, Object> toDetailMap(DriftReport r) {
        return Map.of(
            "id", r.getId(),
            "forgeId", r.getForge().getId(),
            "status", r.getStatus(),
            "driftCount", r.getDriftCount(),
            "startedAt", r.getStartedAt() != null ? r.getStartedAt().toString() : "",
            "completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : "",
            "createdAt", r.getCreatedAt().toString(),
            "items", r.getItems().stream().map(item -> Map.of(
                "nodeId", item.getNodeId(),
                "taskName", item.getTaskName(),
                "status", item.getStatus(),
                "expectedState", item.getExpectedState() != null ? item.getExpectedState() : "",
                "actualState", item.getActualState() != null ? item.getActualState() : ""
            )).collect(Collectors.toList())
        );
    }
}
