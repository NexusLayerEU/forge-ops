package eu.forgeops.domain.forge;

import eu.forgeops.api.dto.ForgeDto;
import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.domain.audit.AuditService;
import eu.forgeops.domain.inventory.Group;
import eu.forgeops.engine.parser.ForgeSpecDocument;
import eu.forgeops.engine.parser.ForgeSpecParser;
import eu.forgeops.engine.validator.ForgeSpecValidator;
import eu.forgeops.engine.validator.ValidationError;
import eu.forgeops.engine.validator.ValidationResult;
import eu.forgeops.infra.persistence.ForgeGroupBindingRepository;
import eu.forgeops.infra.persistence.ForgeRepository;
import eu.forgeops.infra.persistence.ForgeVersionRepository;
import eu.forgeops.infra.persistence.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ForgeService {

    private final ForgeRepository forgeRepo;
    private final ForgeVersionRepository versionRepo;
    private final ForgeGroupBindingRepository bindingRepo;
    private final GroupRepository groupRepo;
    private final ForgeSpecParser parser;
    private final ForgeSpecValidator validator;
    private final AuditService auditService;

    public ForgeDto.Response createForge(ForgeDto.Request req, UUID actorId) {
        if (forgeRepo.existsByName(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Forge name '" + req.getName() + "' already exists");
        }

        ValidationResult validation = validateContent(req.getContent());

        Forge forge = Forge.builder()
            .name(req.getName())
            .description(req.getDescription())
            .mode(req.getMode() != null ? req.getMode() : "mixed")
            .createdBy(actorId)
            .build();

        ForgeVersion version = ForgeVersion.builder()
            .forge(forge)
            .version(1)
            .content(req.getContent())
            .checksum(sha256(req.getContent()))
            .isValid(validation.isValid())
            .errors(validation.getErrorMessages())
            .createdBy(actorId)
            .build();

        forge.getVersions().add(version);
        Forge saved = forgeRepo.save(forge);

        auditService.record(actorId, "FORGE_CREATED", "forge", saved.getId().toString(),
            Map.of("name", saved.getName()), null);

        return toResponse(saved, version);
    }

    @Transactional(readOnly = true)
    public ForgeDto.Response getForge(UUID id) {
        Forge forge = findForge(id);
        ForgeVersion latest = getLatestVersion(forge);
        return toResponse(forge, latest);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForgeDto.Response> listForges(int page, int size, String search, String mode) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<Forge> result;
        if (search != null && !search.isBlank()) {
            result = forgeRepo.findByNameContainingIgnoreCase(search, pageable);
        } else if (mode != null && !mode.isBlank()) {
            result = forgeRepo.findByMode(mode, pageable);
        } else {
            result = forgeRepo.findAll(pageable);
        }
        return PageResponse.<ForgeDto.Response>builder()
            .content(result.getContent().stream()
                .map(f -> toResponse(f, getLatestVersion(f)))
                .collect(Collectors.toList()))
            .totalElements(result.getTotalElements())
            .page(result.getNumber())
            .size(result.getSize())
            .totalPages(result.getTotalPages())
            .build();
    }

    public ForgeDto.Response updateForge(UUID id, ForgeDto.Request req, UUID actorId) {
        Forge forge = findForge(id);
        if (!forge.getName().equals(req.getName()) && forgeRepo.existsByName(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Forge name '" + req.getName() + "' already exists");
        }

        forge.setName(req.getName());
        if (req.getDescription() != null) forge.setDescription(req.getDescription());
        if (req.getMode() != null) forge.setMode(req.getMode());

        ValidationResult validation = validateContent(req.getContent());
        int nextVersion = (versionRepo.findMaxVersionByForgeId(forge.getId()).orElse(0)) + 1;

        ForgeVersion version = ForgeVersion.builder()
            .forge(forge)
            .version(nextVersion)
            .content(req.getContent())
            .checksum(sha256(req.getContent()))
            .isValid(validation.isValid())
            .errors(validation.getErrorMessages())
            .createdBy(actorId)
            .build();

        forge.getVersions().add(version);
        Forge saved = forgeRepo.save(forge);

        auditService.record(actorId, "FORGE_UPDATED", "forge", id.toString(), null, null);
        return toResponse(saved, version);
    }

    public void deleteForge(UUID id, UUID actorId) {
        Forge forge = findForge(id);
        forgeRepo.delete(forge);
        auditService.record(actorId, "FORGE_DELETED", "forge", id.toString(), null, null);
    }

    @Transactional(readOnly = true)
    public List<ForgeDto.VersionResponse> listVersions(UUID forgeId) {
        findForge(forgeId);
        return versionRepo.findAll().stream()
            .filter(v -> v.getForge().getId().equals(forgeId))
            .sorted(Comparator.comparingInt(ForgeVersion::getVersion))
            .map(this::toVersionResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ForgeDto.VersionResponse getVersion(UUID forgeId, int version) {
        return versionRepo.findByForgeIdAndVersion(forgeId, version)
            .map(this::toVersionResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version " + version + " not found"));
    }

    public ForgeDto.ValidateResponse validate(String content) {
        ValidationResult result = validateContent(content);
        return ForgeDto.ValidateResponse.builder()
            .valid(result.isValid())
            .errors(result.getErrorMessages())
            .warnings(result.getWarnings().stream().map(ValidationError::getMessage).collect(Collectors.toList()))
            .build();
    }

    public ForgeDto.BindingResponse addBinding(UUID forgeId, ForgeDto.BindingRequest req, UUID actorId) {
        Forge forge = findForge(forgeId);
        if (bindingRepo.existsByForgeIdAndGroupId(forgeId, req.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Binding already exists for this forge and group");
        }
        Group group = groupRepo.findById(req.getGroupId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        ForgeGroupBinding binding = ForgeGroupBinding.builder()
            .forge(forge)
            .group(group)
            .autoRemediate(req.isAutoRemediate())
            .checkInterval(req.getCheckInterval() > 0 ? req.getCheckInterval() : 3600)
            .build();

        ForgeGroupBinding saved = bindingRepo.save(binding);
        return toBindingResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ForgeDto.BindingResponse> listBindings(UUID forgeId) {
        findForge(forgeId);
        return bindingRepo.findByForgeId(forgeId).stream()
            .map(this::toBindingResponse)
            .collect(Collectors.toList());
    }

    public void removeBinding(UUID forgeId, UUID bindingId, UUID actorId) {
        ForgeGroupBinding binding = bindingRepo.findById(bindingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Binding not found"));
        if (!binding.getForge().getId().equals(forgeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Binding not found for this forge");
        }
        bindingRepo.delete(binding);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    public ValidationResult validateContent(String content) {
        try {
            ForgeSpecDocument doc = parser.parse(content);
            return validator.validate(doc);
        } catch (ForgeSpecParser.ForgeSpecParseException e) {
            ValidationResult result = new ValidationResult();
            result.addError("yaml", "YAML parse error: " + e.getMessage());
            return result;
        }
    }

    public ForgeVersion resolveVersion(UUID forgeId, Integer requestedVersion) {
        Forge forge = findForge(forgeId);
        if (requestedVersion != null) {
            return versionRepo.findByForgeIdAndVersion(forgeId, requestedVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
        }
        if (forge.getPinnedVersion() != null) {
            return versionRepo.findByForgeIdAndVersion(forgeId, forge.getPinnedVersion())
                .orElseGet(() -> getLatestVersion(forge));
        }
        return getLatestVersion(forge);
    }

    public Forge findForge(UUID id) {
        return forgeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Forge not found: " + id));
    }

    private ForgeVersion getLatestVersion(Forge forge) {
        return forge.getVersions().stream()
            .max(Comparator.comparingInt(ForgeVersion::getVersion))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Forge has no versions"));
    }

    private ForgeDto.Response toResponse(Forge forge, ForgeVersion version) {
        return ForgeDto.Response.builder()
            .id(forge.getId())
            .name(forge.getName())
            .description(forge.getDescription())
            .mode(forge.getMode())
            .latestVersion(version != null ? version.getVersion() : 0)
            .pinnedVersion(forge.getPinnedVersion())
            .isValid(version != null && version.isValid())
            .errors(version != null ? version.getErrors() : Collections.emptyList())
            .createdBy(forge.getCreatedBy())
            .createdAt(forge.getCreatedAt())
            .updatedAt(forge.getUpdatedAt())
            .build();
    }

    private ForgeDto.VersionResponse toVersionResponse(ForgeVersion version) {
        return ForgeDto.VersionResponse.builder()
            .id(version.getId())
            .version(version.getVersion())
            .content(version.getContent())
            .checksum(version.getChecksum())
            .isValid(version.isValid())
            .errors(version.getErrors())
            .createdBy(version.getCreatedBy())
            .createdAt(version.getCreatedAt())
            .build();
    }

    private ForgeDto.BindingResponse toBindingResponse(ForgeGroupBinding binding) {
        return ForgeDto.BindingResponse.builder()
            .id(binding.getId())
            .forgeId(binding.getForge().getId())
            .groupId(binding.getGroup().getId())
            .groupName(binding.getGroup().getName())
            .autoRemediate(binding.isAutoRemediate())
            .checkInterval(binding.getCheckInterval())
            .lastCheckedAt(binding.getLastCheckedAt())
            .createdAt(binding.getCreatedAt())
            .build();
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "checksum-error";
        }
    }
}
