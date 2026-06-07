package eu.forgeops.domain.drift;

import eu.forgeops.domain.forge.ForgeGroupBinding;
import eu.forgeops.infra.persistence.ForgeGroupBindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriftScheduler {

    private final ForgeGroupBindingRepository bindingRepository;
    private final DriftService driftService;

    @Scheduled(fixedDelayString = "${forgeops.drift.check-interval-ms:300000}")
    public void scheduledDriftCheck() {
        OffsetDateTime threshold = OffsetDateTime.now();
        List<ForgeGroupBinding> dueBindings = bindingRepository.findDueForCheck(threshold);

        for (ForgeGroupBinding binding : dueBindings) {
            try {
                log.info("Scheduled drift check for forge {}", binding.getForge().getId());
                driftService.startDriftCheck(binding.getForge().getId(), null);
                binding.setLastCheckedAt(OffsetDateTime.now());
                bindingRepository.save(binding);
            } catch (Exception e) {
                log.warn("Scheduled drift check failed for forge {}: {}",
                    binding.getForge().getId(), e.getMessage());
            }
        }
    }
}
