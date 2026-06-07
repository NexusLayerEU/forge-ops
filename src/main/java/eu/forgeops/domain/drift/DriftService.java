package eu.forgeops.domain.drift;

import eu.forgeops.domain.forge.Forge;
import eu.forgeops.domain.forge.ForgeVersion;
import eu.forgeops.infra.persistence.DriftReportRepository;
import eu.forgeops.infra.persistence.ForgeRepository;
import eu.forgeops.infra.persistence.ForgeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class DriftService {

    private final DriftReportRepository driftReportRepository;
    private final ForgeRepository forgeRepository;
    private final ForgeVersionRepository forgeVersionRepository;
    private final DriftEngine driftEngine;

    @Transactional
    public DriftReport startDriftCheck(UUID forgeId, UUID triggeredBy) {
        Forge forge = forgeRepository.findById(forgeId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Forge not found: " + forgeId));

        ForgeVersion version = forgeVersionRepository.findTopByForgeIdOrderByVersionDesc(forgeId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No forge version found for forge: " + forgeId));

        DriftReport report = DriftReport.builder()
            .forge(forge)
            .triggeredBy(triggeredBy)
            .status("pending")
            .build();
        report = driftReportRepository.save(report);

        driftEngine.checkDrift(report, forge, version);
        return report;
    }

    @Transactional(readOnly = true)
    public Page<DriftReport> listByForge(UUID forgeId, Pageable pageable) {
        return driftReportRepository.findByForgeIdOrderByCreatedAtDesc(forgeId, pageable);
    }

    @Transactional(readOnly = true)
    public DriftReport getById(UUID id) {
        return driftReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Drift report not found: " + id));
    }
}
