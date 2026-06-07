package eu.forgeops.infra.persistence;

import eu.forgeops.domain.inventory.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByName(String name);

    boolean existsByName(String name);

    List<Group> findByParentIsNull();

    List<Group> findByParentId(UUID parentId);

    Page<Group> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE :nodeId IN (SELECT n.id FROM g.members n)")
    List<Group> findGroupsByNodeId(@Param("nodeId") UUID nodeId);
}
