package eu.forgeops.domain.audit;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

@Entity
@Table(name = "audit_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 64)
    private String resource;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;
}
