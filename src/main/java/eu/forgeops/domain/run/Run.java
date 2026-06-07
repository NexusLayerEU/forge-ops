package eu.forgeops.domain.run;

import eu.forgeops.domain.forge.ForgeVersion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "tasks")
public class Run {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forge_version_id")
    private ForgeVersion forgeVersion;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "trigger_type", length = 16)
    @Builder.Default
    private String triggerType = "manual";

    @Builder.Default
    private String status = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_nodes", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> targetNodes = new ArrayList<>();

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> summary = new HashMap<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RunTask> tasks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
