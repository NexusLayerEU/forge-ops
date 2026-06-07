package eu.forgeops.infra.persistence;

import eu.forgeops.domain.vault.Secret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecretRepository extends JpaRepository<Secret, UUID> {
    Optional<Secret> findByName(String name);
    boolean existsByName(String name);
}
