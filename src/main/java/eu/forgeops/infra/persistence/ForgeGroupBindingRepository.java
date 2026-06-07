package eu.forgeops.infra.persistence;

import eu.forgeops.domain.forge.ForgeGroupBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForgeGroupBindingRepository extends JpaRepository<ForgeGroupBinding, UUID> {
    List<ForgeGroupBinding> findByForgeId(UUID forgeId);
    Optional<ForgeGroupBinding> findByForgeIdAndGroupId(UUID forgeId, UUID groupId);
    boolean existsByForgeIdAndGroupId(UUID forgeId, UUID groupId);

    @Query("SELECT b FROM ForgeGroupBinding b WHERE b.lastCheckedAt IS NULL OR " +
           "b.lastCheckedAt < :threshold")
    List<ForgeGroupBinding> findDueForCheck(@Param("threshold") OffsetDateTime threshold);
}
