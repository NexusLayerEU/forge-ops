package eu.forgeops.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.*;

public class NodeDto {

    @Data
    @Builder
    public static class Request {
        @NotBlank
        private String name;

        @NotBlank
        private String hostname;

        @Min(1) @Max(65535)
        private Integer port;

        @Pattern(regexp = "linux|windows")
        private String osType;

        @Pattern(regexp = "ssh|winrm")
        private String connectionType;

        private UUID credentialId;
        private String description;
        private Map<String, Object> tags;
    }

    @Data
    @Builder
    public static class Response {
        private UUID id;
        private String name;
        private String hostname;
        private int port;
        private String osType;
        private String connectionType;
        private UUID credentialId;
        private String description;
        private Map<String, Object> tags;
        private OffsetDateTime lastSeenAt;
        private String status;
        private List<String> groups;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    public static class PingResponse {
        private boolean reachable;
        private Long latencyMs;
        private String message;
    }
}
