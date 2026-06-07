package eu.forgeops.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

public class VariableDto {

    @Data
    @Builder
    public static class Request {
        @NotBlank
        private String key;

        @NotBlank
        private String value;

        private boolean isSecret;
    }

    @Data
    @Builder
    public static class Response {
        private UUID id;
        private String key;
        private String value;
        private boolean isSecret;
    }
}
