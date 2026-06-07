package eu.forgeops.infra.persistence;

import eu.forgeops.domain.run.RunLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RunLogRepository extends JpaRepository<RunLog, Long> {

    @Query("SELECT rl FROM RunLog rl WHERE rl.runTask.run.id = :runId ORDER BY rl.lineNo ASC")
    List<RunLog> findByRunId(UUID runId);

    @Query("SELECT rl FROM RunLog rl WHERE rl.runTask.id = :taskId ORDER BY rl.lineNo ASC")
    List<RunLog> findByTaskId(UUID taskId);
}
