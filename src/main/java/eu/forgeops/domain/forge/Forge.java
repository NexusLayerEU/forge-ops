package eu.forgeops.domain.forge;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "forges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"versions", "bindings"})
public class Forge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private String mode = "mixed";

    @Column(name = "pinned_version")
    private Integer pinnedVersion;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "forge", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version ASC")
    @Builder.Default
    private List<ForgeVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "forge", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ForgeGroupBinding> bindings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
