package eu.forgeops.infra.persistence;

import eu.forgeops.domain.run.Run;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RunRepository extends JpaRepository<Run, UUID> {
    Page<Run> findByForgeVersion_Forge_IdOrderByCreatedAtDesc(UUID forgeId, Pageable pageable);
    List<Run> findByStatusIn(List<String> statuses);

    @Query("SELECT r FROM Run r WHERE r.forgeVersion.id = :forgeVersionId ORDER BY r.createdAt DESC")
    Page<Run> findByForgeVersionId(UUID forgeVersionId, Pageable pageable);
}
