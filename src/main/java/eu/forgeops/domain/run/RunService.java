package eu.forgeops.domain.run;

import eu.forgeops.api.websocket.RunOutputRelay;
import eu.forgeops.domain.forge.ForgeVersion;
import eu.forgeops.engine.executor.ParallelNodeExecutor;
import eu.forgeops.engine.parser.ForgeSpecDocument;
import eu.forgeops.engine.parser.ForgeSpecParser;
import eu.forgeops.infra.persistence.ForgeVersionRepository;
import eu.forgeops.infra.persistence.RunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunService {

    private static final String CANCEL_KEY = "forgeops:runs:%s:cancel";

    private final RunRepository runRepository;
    private final ForgeVersionRepository forgeVersionRepository;
    private final ForgeSpecParser forgeSpecParser;
    private final ParallelNodeExecutor executor;
    private final RunOutputRelay outputRelay;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public Run createRun(UUID forgeVersionId, List<UUID> targetNodeIds, UUID triggeredBy, String triggerType) {
        ForgeVersion version = forgeVersionRepository.findById(forgeVersionId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Forge version not found: " + forgeVersionId));

        Run run = Run.builder()
            .forgeVersion(version)
            .triggeredBy(triggeredBy)
            .triggerType(triggerType != null ? triggerType : "manual")
            .targetNodes(targetNodeIds)
            .status("pending")
            .build();

        return runRepository.save(run);
    }

    @Async
    public void executeRunAsync(UUID runId) {
        Run run = runRepository.findById(runId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Run not found: " + runId));

        outputRelay.subscribe(runId);
        try {
            ForgeSpecDocument spec = forgeSpecParser.parse(run.getForgeVersion().getContent());
            executor.execute(run, spec);
        } catch (Exception e) {
            log.error("Run {} failed unexpectedly", runId, e);
            run.setStatus("failed");
            run.setCompletedAt(OffsetDateTime.now());
            runRepository.save(run);
        } finally {
            outputRelay.unsubscribe(runId);
        }
    }

    @Transactional
    public void cancelRun(UUID runId) {
        Run run = runRepository.findById(runId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Run not found: " + runId));

        if (!List.of("pending", "running").contains(run.getStatus())) {
            throw new ResponseStatusException(CONFLICT, "Run is not in a cancellable state: " + run.getStatus());
        }
        // Signal cancellation via Redis
        redisTemplate.opsForValue().set(CANCEL_KEY.formatted(runId), "1");
    }

    @Transactional(readOnly = true)
    public Run getById(UUID id) {
        return runRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Run not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Run> listByForge(UUID forgeId, Pageable pageable) {
        return runRepository.findByForgeVersion_Forge_IdOrderByCreatedAtDesc(forgeId, pageable);
    }
}
