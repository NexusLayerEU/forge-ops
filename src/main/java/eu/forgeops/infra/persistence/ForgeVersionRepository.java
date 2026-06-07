package eu.forgeops.infra.persistence;

import eu.forgeops.domain.forge.ForgeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForgeVersionRepository extends JpaRepository<ForgeVersion, UUID> {
    Optional<ForgeVersion> findByForgeIdAndVersion(UUID forgeId, int version);

    @Query("SELECT MAX(fv.version) FROM ForgeVersion fv WHERE fv.forge.id = :forgeId")
    Optional<Integer> findMaxVersionByForgeId(@Param("forgeId") UUID forgeId);

    Optional<ForgeVersion> findTopByForgeIdOrderByVersionDesc(UUID forgeId);
}
