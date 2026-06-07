package eu.forgeops.api.controller;

import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.api.dto.SecretDto;
import eu.forgeops.api.dto.SecretDto.SecretResponse;
import eu.forgeops.domain.vault.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vault/secrets")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<SecretResponse> create(
        @Valid @RequestBody SecretDto.CreateSecretRequest req,
        @AuthenticationPrincipal UserDetails principal
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        var secret = vaultService.createSecret(req.name(), req.secretType(), req.value(), req.description(), userId);
        return ResponseEntity
            .created(URI.create("/api/v1/vault/secrets/" + secret.getId()))
            .body(SecretResponse.from(secret));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public PageResponse<SecretResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.from(vaultService.list(pageable).map(SecretResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public SecretResponse get(@PathVariable UUID id) {
        return SecretResponse.from(vaultService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public SecretResponse update(
        @PathVariable UUID id,
        @RequestBody SecretDto.UpdateSecretRequest req,
        @AuthenticationPrincipal UserDetails principal
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return SecretResponse.from(vaultService.updateSecret(id, req.value(), req.description(), userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vaultService.deleteSecret(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/reveal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reveal(@PathVariable UUID id) {
        String value = vaultService.reveal(id);
        return ResponseEntity.ok(Map.of("value", value));
    }
}
