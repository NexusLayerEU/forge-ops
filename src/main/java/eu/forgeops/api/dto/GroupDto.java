package eu.forgeops.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.*;

public class GroupDto {

    @Data
    @Builder
    public static class Request {
        @NotBlank
        private String name;
        private String description;
        private UUID parentId;
    }

    @Data
    @Builder
    public static class Response {
        private UUID id;
        private String name;
        private String description;
        private UUID parentId;
        private String parentName;
        private int memberCount;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    public static class MembersRequest {
        private List<UUID> nodeIds;
    }
}
