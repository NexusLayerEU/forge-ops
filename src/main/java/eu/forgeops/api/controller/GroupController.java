package eu.forgeops.api.controller;

import eu.forgeops.api.dto.GroupDto;
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

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<PageResponse<GroupDto.Response>> listGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(inventoryService.listGroups(page, size, search));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<GroupDto.Response> createGroup(
            @Valid @RequestBody GroupDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.createGroup(req, actorId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDto.Response> getGroup(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.getGroup(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<GroupDto.Response> updateGroup(
            @PathVariable UUID id,
            @Valid @RequestBody GroupDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(inventoryService.updateGroup(id, req, actorId, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable UUID id,
            @AuthenticationPrincipal(expression = "id") UUID actorId,
            HttpServletRequest httpRequest) {
        inventoryService.deleteGroup(id, actorId, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<Void> addMembers(
            @PathVariable UUID id,
            @RequestBody GroupDto.MembersRequest req,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        inventoryService.addNodesToGroup(id, req.getNodeIds(), actorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID nodeId,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        inventoryService.removeNodeFromGroup(id, nodeId, actorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/variables")
    public ResponseEntity<List<VariableDto.Response>> getVariables(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.getGroupVariables(id));
    }

    @PostMapping("/{id}/variables")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<VariableDto.Response> addVariable(
            @PathVariable UUID id,
            @Valid @RequestBody VariableDto.Request req,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.addGroupVariable(id, req, actorId));
    }

    @DeleteMapping("/{id}/variables/{variableId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','NODE_MANAGER')")
    public ResponseEntity<Void> deleteVariable(
            @PathVariable UUID id,
            @PathVariable UUID variableId,
            @AuthenticationPrincipal(expression = "id") UUID actorId) {
        inventoryService.deleteGroupVariable(id, variableId, actorId);
        return ResponseEntity.noContent().build();
    }
}
