package eu.forgeops.infra.persistence;

import eu.forgeops.domain.inventory.NodeVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NodeVariableRepository extends JpaRepository<NodeVariable, UUID> {

    List<NodeVariable> findByNodeId(UUID nodeId);

    Optional<NodeVariable> findByNodeIdAndKey(UUID nodeId, String key);

    boolean existsByNodeIdAndKey(UUID nodeId, String key);

    void deleteByNodeId(UUID nodeId);
}
