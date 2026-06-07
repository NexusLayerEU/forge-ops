package eu.forgeops.infra.persistence;

import eu.forgeops.domain.inventory.Node;
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
public interface NodeRepository extends JpaRepository<Node, UUID> {

    Optional<Node> findByName(String name);

    boolean existsByName(String name);

    Page<Node> findByStatus(String status, Pageable pageable);

    Page<Node> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT n FROM Node n WHERE (:search IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.hostname) LIKE LOWER(CONCAT('%', :search, '%'))) AND (:status IS NULL OR n.status = :status)")
    Page<Node> searchNodes(@Param("search") String search, @Param("status") String status, Pageable pageable);

    @Query("SELECT n FROM Node n JOIN n.groups g WHERE g.id = :groupId")
    Page<Node> findByGroupId(@Param("groupId") UUID groupId, Pageable pageable);

    @Query("SELECT n FROM Node n JOIN n.groups g WHERE g.id = :groupId")
    List<Node> findAllByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT DISTINCT n FROM Node n LEFT JOIN FETCH n.variables LEFT JOIN FETCH n.groups g LEFT JOIN FETCH g.variables WHERE n.id IN :ids")
    List<Node> findAllWithDetailsByIdIn(@Param("ids") List<UUID> ids);
}
