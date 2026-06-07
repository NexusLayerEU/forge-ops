package eu.forgeops.domain.run;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "run_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"run", "logs"})
public class RunTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private Run run;

    @Column(name = "node_id")
    private UUID nodeId;

    @Column(name = "task_index")
    private int taskIndex;

    @Column(name = "task_name")
    private String taskName;

    @Column(length = 64)
    private String module;

    @Builder.Default
    private String status = "pending";

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "exit_code")
    private Integer exitCode;

    @OneToMany(mappedBy = "runTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    @Builder.Default
    private List<RunLog> logs = new ArrayList<>();
}
