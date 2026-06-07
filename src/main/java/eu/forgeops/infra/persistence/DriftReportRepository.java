package eu.forgeops.infra.persistence;

import eu.forgeops.domain.drift.DriftReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DriftReportRepository extends JpaRepository<DriftReport, UUID> {
    Page<DriftReport> findByForgeIdOrderByCreatedAtDesc(UUID forgeId, Pageable pageable);
}
