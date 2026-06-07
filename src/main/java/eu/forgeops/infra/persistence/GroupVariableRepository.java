package eu.forgeops.infra.persistence;

import eu.forgeops.domain.inventory.GroupVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupVariableRepository extends JpaRepository<GroupVariable, UUID> {

    List<GroupVariable> findByGroupId(UUID groupId);

    boolean existsByGroupIdAndKey(UUID groupId, String key);
}
