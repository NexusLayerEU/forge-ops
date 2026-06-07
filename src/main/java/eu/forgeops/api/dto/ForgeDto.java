package eu.forgeops.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ForgeDto {

    @Data
    @Builder
    public static class Request {
        @NotBlank
        private String name;
        private String description;
        private String mode;
        @NotBlank
        private String content;
    }

    @Data
    @Builder
    public static class Response {
        private UUID id;
        private String name;
        private String description;
        private String mode;
        private int latestVersion;
        private Integer pinnedVersion;
        private boolean isValid;
        private List<String> errors;
        private UUID createdBy;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    public static class VersionResponse {
        private UUID id;
        private int version;
        private String content;
        private String checksum;
        private boolean isValid;
        private List<String> errors;
        private UUID createdBy;
        private OffsetDateTime createdAt;
    }

    @Data
    @Builder
    public static class ValidateRequest {
        private String content;
    }

    @Data
    @Builder
    public static class ValidateResponse {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
    }

    @Data
    @Builder
    public static class BindingRequest {
        private UUID groupId;
        private boolean autoRemediate;
        private int checkInterval;
    }

    @Data
    @Builder
    public static class BindingResponse {
        private UUID id;
        private UUID forgeId;
        private UUID groupId;
        private String groupName;
        private boolean autoRemediate;
        private int checkInterval;
        private OffsetDateTime lastCheckedAt;
        private OffsetDateTime createdAt;
    }
}
