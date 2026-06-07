package eu.forgeops.api.dto;

import eu.forgeops.domain.vault.Secret;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SecretDto {

    public record SecretResponse(
        UUID id,
        String name,
        String secretType,
        String description,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        public static SecretResponse from(Secret s) {
            return new SecretResponse(
                s.getId(), s.getName(),
                s.getSecretType() != null ? s.getSecretType().name() : null,
                s.getDescription(), s.getCreatedBy(),
                s.getCreatedAt(), s.getUpdatedAt()
            );
        }
    }

    public record CreateSecretRequest(
        @NotBlank String name,
        @NotNull Secret.SecretType secretType,
        @NotBlank String value,
        String description
    ) {}

    public record UpdateSecretRequest(
        String value,
        String description
    ) {}
}
