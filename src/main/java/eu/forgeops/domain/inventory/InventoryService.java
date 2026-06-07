package eu.forgeops.domain.inventory;

import eu.forgeops.api.dto.GroupDto;
import eu.forgeops.api.dto.NodeDto;
import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.api.dto.VariableDto;
import eu.forgeops.domain.audit.AuditService;
import eu.forgeops.infra.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryService {

    private final NodeRepository nodeRepo;
    private final GroupRepository groupRepo;
    private final NodeVariableRepository nodeVarRepo;
    private final GroupVariableRepository groupVarRepo;
    private final AuditService auditService;

    // ── Nodes ──────────────────────────────────────────────────────────────

    public NodeDto.Response createNode(NodeDto.Request req, UUID actorId, String ipAddress) {
        if (nodeRepo.existsByName(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Node name '" + req.getName() + "' already exists");
        }
        Node node = Node.builder()
            .name(req.getName())
            .hostname(req.getHostname())
            .port(req.getPort() != null ? req.getPort() : 22)
            .osType(req.getOsType() != null ? req.getOsType() : "linux")
            .connectionType(req.getConnectionType() != null ? req.getConnectionType() : "ssh")
            .description(req.getDescription())
            .tags(req.getTags() != null ? req.getTags() : new HashMap<>())
            .status("unknown")
            .build();
        Node saved = nodeRepo.save(node);
        auditService.record(actorId, "NODE_CREATED", "node", saved.getId().toString(),
            Map.of("name", saved.getName()), ipAddress);
        return toNodeResponse(saved);
    }

    @Transactional(readOnly = true)
    public NodeDto.Response getNode(UUID id) {
        return toNodeResponse(findNode(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<NodeDto.Response> listNodes(int page, int size, String search, String status, UUID groupId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<Node> result;
        if (groupId != null) {
            result = nodeRepo.findByGroupId(groupId, pageable);
        } else {
            result = nodeRepo.searchNodes(search, status, pageable);
        }
        return PageResponse.<NodeDto.Response>builder()
            .content(result.getContent().stream().map(this::toNodeResponse).collect(Collectors.toList()))
            .totalElements(result.getTotalElements())
            .page(result.getNumber())
            .size(result.getSize())
            .totalPages(result.getTotalPages())
            .build();
    }

    public NodeDto.Response updateNode(UUID id, NodeDto.Request req, UUID actorId, String ipAddress) {
        Node node = findNode(id);
        if (!node.getName().equals(req.getName()) && nodeRepo.existsByName(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Node name '" + req.getName() + "' already exists");
        }
        node.setName(req.getName());
        node.setHostname(req.getHostname());
        if (req.getPort() != null) node.setPort(req.getPort());
        if (req.getOsType() != null) node.setOsType(req.getOsType());
        if (req.getConnectionType() != null) node.setConnectionType(req.getConnectionType());
        if (req.getDescription() != null) node.setDescription(req.getDescription());
        if (req.getTags() != null) node.setTags(req.getTags());
        Node saved = nodeRepo.save(node);
        auditService.record(actorId, "NODE_UPDATED", "node", id.toString(), null, ipAddress);
        return toNodeResponse(saved);
    }

    public void deleteNode(UUID id, UUID actorId, String ipAddress) {
        Node node = findNode(id);
        nodeRepo.delete(node);
        auditService.record(actorId, "NODE_DELETED", "node", id.toString(), null, ipAddress);
    }

    @Async
    public CompletableFuture<NodeDto.PingResponse> pingNode(UUID id) {
        Node node = findNode(id);
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(node.getHostname(), node.getPort()), 5000);
            long latency = System.currentTimeMillis() - start;
            updateNodeStatus(id, "reachable");
            return CompletableFuture.completedFuture(
                NodeDto.PingResponse.builder().reachable(true).latencyMs(latency).build());
        } catch (Exception e) {
            updateNodeStatus(id, "unreachable");
            return CompletableFuture.completedFuture(
                NodeDto.PingResponse.builder().reachable(false).message(e.getMessage()).build());
        }
    }

    @Transactional
    public void updateNodeStatus(UUID id, String status) {
        nodeRepo.findById(id).ifPresent(n -> {
            n.setStatus(status);
            n.setLastSeenAt(OffsetDateTime.now());
            nodeRepo.save(n);
        });
    }

    // ── Node Variables ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VariableDto.Response> getNodeVariables(UUID nodeId) {
        findNode(nodeId);
        return nodeVarRepo.findByNodeId(nodeId).stream().map(this::toVarResponse).collect(Collectors.toList());
    }

    public VariableDto.Response addNodeVariable(UUID nodeId, VariableDto.Request req, UUID actorId) {
        Node node = findNode(nodeId);
        if (nodeVarRepo.existsByNodeIdAndKey(nodeId, req.getKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Variable key '" + req.getKey() + "' already exists on this node");
        }
        NodeVariable variable = NodeVariable.builder()
            .node(node)
            .key(req.getKey())
            .value(req.getValue())
            .isSecret(req.isSecret())
            .build();
        return toVarResponse(nodeVarRepo.save(variable));
    }

    public void deleteNodeVariable(UUID nodeId, UUID variableId, UUID actorId) {
        findNode(nodeId);
        NodeVariable v = nodeVarRepo.findById(variableId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variable not found"));
        nodeVarRepo.delete(v);
    }

    // ── Groups ─────────────────────────────────────────────────────────────

    public GroupDto.Response createGroup(GroupDto.Request req, UUID actorId, String ipAddress) {
        if (groupRepo.existsByName(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name '" + req.getName() + "' already exists");
        }
        Group parent = null;
        if (req.getParentId() != null) {
            parent = findGroup(req.getParentId());
        }
        Group group = Group.builder()
            .name(req.getName())
            .description(req.getDescription())
            .parent(parent)
            .build();
        Group saved = groupRepo.save(group);
        auditService.record(actorId, "GROUP_CREATED", "group", saved.getId().toString(),
            Map.of("name", saved.getName()), ipAddress);
        return toGroupResponse(saved);
    }

    @Transactional(readOnly = true)
    public GroupDto.Response getGroup(UUID id) {
        return toGroupResponse(findGroup(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupDto.Response> listGroups(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<Group> result = (search != null && !search.isBlank())
            ? groupRepo.findByNameContainingIgnoreCase(search, pageable)
            : groupRepo.findAll(pageable);
        return PageResponse.<GroupDto.Response>builder()
            .content(result.getContent().stream().map(this::toGroupResponse).collect(Collectors.toList()))
            .totalElements(result.getTotalElements())
            .page(result.getNumber())
            .size(result.getSize())
            .totalPages(result.getTotalPages())
            .build();
    }

    public GroupDto.Response updateGroup(UUID id, GroupDto.Request req, UUID actorId, String ipAddress) {
        Group group = findGroup(id);
        if (!group.getName().equals(req.getName()) && groupRepo.existsByName(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name '" + req.getName() + "' already exists");
        }
        group.setName(req.getName());
        group.setDescription(req.getDescription());
        if (req.getParentId() != null) {
            group.setParent(findGroup(req.getParentId()));
        } else {
            group.setParent(null);
        }
        Group saved = groupRepo.save(group);
        auditService.record(actorId, "GROUP_UPDATED", "group", id.toString(), null, ipAddress);
        return toGroupResponse(saved);
    }

    public void deleteGroup(UUID id, UUID actorId, String ipAddress) {
        Group group = findGroup(id);
        groupRepo.delete(group);
        auditService.record(actorId, "GROUP_DELETED", "group", id.toString(), null, ipAddress);
    }

    public void addNodesToGroup(UUID groupId, List<UUID> nodeIds, UUID actorId) {
        Group group = findGroup(groupId);
        for (UUID nodeId : nodeIds) {
            Node node = findNode(nodeId);
            node.getGroups().add(group);
            nodeRepo.save(node);
        }
    }

    public void removeNodeFromGroup(UUID groupId, UUID nodeId, UUID actorId) {
        Group group = findGroup(groupId);
        Node node = findNode(nodeId);
        node.getGroups().remove(group);
        nodeRepo.save(node);
    }

    // ── Group Variables ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VariableDto.Response> getGroupVariables(UUID groupId) {
        findGroup(groupId);
        return groupVarRepo.findByGroupId(groupId).stream().map(v ->
            VariableDto.Response.builder()
                .id(v.getId()).key(v.getKey()).value(v.getValue()).isSecret(v.isSecret()).build()
        ).collect(Collectors.toList());
    }

    public VariableDto.Response addGroupVariable(UUID groupId, VariableDto.Request req, UUID actorId) {
        Group group = findGroup(groupId);
        if (groupVarRepo.existsByGroupIdAndKey(groupId, req.getKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Variable key '" + req.getKey() + "' already exists on this group");
        }
        GroupVariable variable = GroupVariable.builder()
            .group(group).key(req.getKey()).value(req.getValue()).isSecret(req.isSecret()).build();
        GroupVariable saved = groupVarRepo.save(variable);
        return VariableDto.Response.builder()
            .id(saved.getId()).key(saved.getKey()).value(saved.getValue()).isSecret(saved.isSecret()).build();
    }

    public void deleteGroupVariable(UUID groupId, UUID variableId, UUID actorId) {
        findGroup(groupId);
        GroupVariable v = groupVarRepo.findById(variableId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variable not found"));
        groupVarRepo.delete(v);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Node findNode(UUID id) {
        return nodeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found: " + id));
    }

    private Group findGroup(UUID id) {
        return groupRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found: " + id));
    }

    private NodeDto.Response toNodeResponse(Node node) {
        return NodeDto.Response.builder()
            .id(node.getId())
            .name(node.getName())
            .hostname(node.getHostname())
            .port(node.getPort())
            .osType(node.getOsType())
            .connectionType(node.getConnectionType())
            .credentialId(node.getCredential() != null ? node.getCredential().getId() : null)
            .description(node.getDescription())
            .tags(node.getTags())
            .lastSeenAt(node.getLastSeenAt())
            .status(node.getStatus())
            .groups(node.getGroups().stream().map(Group::getName).collect(Collectors.toList()))
            .createdAt(node.getCreatedAt())
            .updatedAt(node.getUpdatedAt())
            .build();
    }

    private GroupDto.Response toGroupResponse(Group group) {
        return GroupDto.Response.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            .parentId(group.getParent() != null ? group.getParent().getId() : null)
            .parentName(group.getParent() != null ? group.getParent().getName() : null)
            .memberCount(group.getMembers().size())
            .createdAt(group.getCreatedAt())
            .updatedAt(group.getUpdatedAt())
            .build();
    }

    private VariableDto.Response toVarResponse(NodeVariable v) {
        return VariableDto.Response.builder()
            .id(v.getId()).key(v.getKey()).value(v.getValue()).isSecret(v.isSecret()).build();
    }
}
