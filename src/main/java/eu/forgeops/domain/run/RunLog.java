package eu.forgeops.domain.run;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "run_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_task_id", nullable = false)
    @ToString.Exclude
    private RunTask runTask;

    @Column(name = "line_no")
    private int lineNo;

    @Column(length = 8)
    @Builder.Default
    private String level = "info";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "logged_at")
    private OffsetDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = OffsetDateTime.now();
    }
}
