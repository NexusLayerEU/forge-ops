package eu.forgeops.domain.drift;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "drift_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @ToString.Exclude
    private DriftReport report;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "drifted";

    @Column(name = "expected_state", columnDefinition = "TEXT")
    private String expectedState;

    @Column(name = "actual_state", columnDefinition = "TEXT")
    private String actualState;
}
