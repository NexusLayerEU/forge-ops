package eu.forgeops.api.controller;

import eu.forgeops.api.dto.NodeDto;
import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.api.dto.VariableDto;
import eu.forgeops.domain.inventory.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<PageResponse<NodeDto.Response>> listNodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID groupId) {
        return ResponseEntity.ok(inventoryService.listNodes(page, size, search, status, groupId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<NodeDto.Response> createNode(
            @Valid @RequestBody NodeDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.createNode(req, actorId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeDto.Response> getNode(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.getNode(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<NodeDto.Response> updateNode(
            @PathVariable UUID id,
            @Valid @RequestBody NodeDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(inventoryService.updateNode(id, req, actorId, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<Void> deleteNode(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "id") UUID actorId,
            HttpServletRequest httpRequest) {
        inventoryService.deleteNode(id, actorId, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ping")
    public CompletableFuture<ResponseEntity<NodeDto.PingResponse>> pingNode(@PathVariable UUID id) {
        return inventoryService.pingNode(id)
            .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}/variables")
    public ResponseEntity<List<VariableDto.Response>> getVariables(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.getNodeVariables(id));
    }

    @PostMapping("/{id}/variables")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<VariableDto.Response> addVariable(
            @PathVariable UUID id,
            @Valid @RequestBody VariableDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.addNodeVariable(id, req, actorId));
    }

    @DeleteMapping("/{id}/variables/{variableId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<Void> deleteVariable(
            @PathVariable UUID id,
            @PathVariable UUID variableId,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        inventoryService.deleteNodeVariable(id, variableId, actorId);
        return ResponseEntity.noContent().build();
    }
}
