package eu.forgeops.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByUserId(UUID userId, Pageable pageable);

    Page<AuditEvent> findByResource(String resource, Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:resource IS NULL OR a.resource = :resource) AND " +
           "(:from IS NULL OR a.occurredAt >= :from) AND " +
           "(:to IS NULL OR a.occurredAt <= :to)")
    Page<AuditEvent> search(@Param("userId") UUID userId,
                             @Param("resource") String resource,
                             @Param("from") OffsetDateTime from,
                             @Param("to") OffsetDateTime to,
                             Pageable pageable);
}
