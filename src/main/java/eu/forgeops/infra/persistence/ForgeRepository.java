package eu.forgeops.infra.persistence;

import eu.forgeops.domain.forge.Forge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForgeRepository extends JpaRepository<Forge, UUID> {
    Optional<Forge> findByName(String name);
    boolean existsByName(String name);
    Page<Forge> findByMode(String mode, Pageable pageable);
    Page<Forge> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
