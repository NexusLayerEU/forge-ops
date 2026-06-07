package eu.forgeops.infra.persistence;

import eu.forgeops.domain.run.RunTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RunTaskRepository extends JpaRepository<RunTask, UUID> {
    List<RunTask> findByRunIdOrderByStartedAtAsc(UUID runId);
}
