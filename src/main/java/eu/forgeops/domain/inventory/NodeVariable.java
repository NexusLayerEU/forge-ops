package eu.forgeops.domain.inventory;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "node_variables", uniqueConstraints = @UniqueConstraint(columnNames = {"node_id", "key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class NodeVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    @ToString.Exclude
    private Node node;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "is_secret")
    @Builder.Default
    private boolean isSecret = false;
}
