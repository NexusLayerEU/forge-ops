package eu.forgeops.api.controller;

import eu.forgeops.api.dto.ForgeDto;
import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.domain.forge.ForgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forges")
@RequiredArgsConstructor
public class ForgeController {

    private final ForgeService forgeService;

    @GetMapping
    public ResponseEntity<PageResponse<ForgeDto.Response>> listForges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String mode) {
        return ResponseEntity.ok(forgeService.listForges(page, size, search, mode));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ForgeDto.Response> createForge(
            @Valid @RequestBody ForgeDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(forgeService.createForge(req, actorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForgeDto.Response> getForge(@PathVariable UUID id) {
        return ResponseEntity.ok(forgeService.getForge(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ForgeDto.Response> updateForge(
            @PathVariable UUID id,
            @Valid @RequestBody ForgeDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        return ResponseEntity.ok(forgeService.updateForge(id, req, actorId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> deleteForge(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        forgeService.deleteForge(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ForgeDto.VersionResponse>> listVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(forgeService.listVersions(id));
    }

    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<ForgeDto.VersionResponse> getVersion(
            @PathVariable UUID id, @PathVariable int version) {
        return ResponseEntity.ok(forgeService.getVersion(id, version));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ForgeDto.ValidateResponse> validate(
            @PathVariable UUID id,
            @RequestBody ForgeDto.ValidateRequest req) {
        return ResponseEntity.ok(forgeService.validate(req.getContent()));
    }

    @GetMapping("/{id}/bindings")
    public ResponseEntity<List<ForgeDto.BindingResponse>> listBindings(@PathVariable UUID id) {
        return ResponseEntity.ok(forgeService.listBindings(id));
    }

    @PostMapping("/{id}/bindings")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ForgeDto.BindingResponse> addBinding(
            @PathVariable UUID id,
            @RequestBody ForgeDto.BindingRequest req,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(forgeService.addBinding(id, req, actorId));
    }

    @DeleteMapping("/{id}/bindings/{bindingId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> removeBinding(
            @PathVariable UUID id,
            @PathVariable UUID bindingId,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        forgeService.removeBinding(id, bindingId, actorId);
        return ResponseEntity.noContent().build();
    }
}
