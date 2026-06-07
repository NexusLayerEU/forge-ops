package eu.forgeops.api.controller;

import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.domain.audit.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditRepository;

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public PageResponse<Map<String, Object>> listEvents(
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) String resource,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());
        return PageResponse.from(
            auditRepository.search(userId, resource, from, to, pageable)
                .map(event -> Map.of(
                    "id", event.getId(),
                    "userId", event.getUserId() != null ? event.getUserId().toString() : "",
                    "action", event.getAction(),
                    "resource", event.getResource(),
                    "resourceId", event.getResourceId() != null ? event.getResourceId() : "",
                    "ipAddress", event.getIpAddress() != null ? event.getIpAddress() : "",
                    "occurredAt", event.getOccurredAt().toString()
                ))
        );
    }
}
