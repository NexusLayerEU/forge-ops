package eu.forgeops.domain.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String action, String resource, String resourceId, Map<String, Object> payload, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
            .userId(userId)
            .action(action)
            .resource(resource)
            .resourceId(resourceId)
            .payload(payload != null ? payload : new HashMap<>())
            .ipAddress(ipAddress)
            .occurredAt(OffsetDateTime.now())
            .build();
        repository.save(event);
        log.debug("Audit: {} {} {} by {}", action, resource, resourceId, userId);
    }

    public void record(UUID userId, String action, String resource, String resourceId) {
        record(userId, action, resource, resourceId, null, null);
    }
}
