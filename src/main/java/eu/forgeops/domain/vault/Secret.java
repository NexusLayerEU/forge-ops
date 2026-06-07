package eu.forgeops.domain.vault;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "secrets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Secret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String name;

    @Column(name = "secret_type", length = 32)
    @Enumerated(EnumType.STRING)
    private SecretType secretType;

    @Column(name = "encrypted_value", nullable = false)
    private byte[] encryptedValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum SecretType {
        ssh_key, password, token, certificate
    }
}
