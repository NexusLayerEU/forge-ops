package eu.forgeops.engine.parser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.*;

/**
 * Root object model for a ForgeSpec YAML document.
 * Maps directly to the DSL structure defined in docs/04_forgespec_dsl.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForgeSpecDocument {

    @JsonProperty("forgespec")
    private String forgespecVersion;

    private String name;
    private String description;
    private String mode;

    @Builder.Default
    private Map<String, Object> vars = new LinkedHashMap<>();

    private ForgeTarget targets;

    @Builder.Default
    private List<ForgeTask> tasks = new ArrayList<>();

    @Builder.Default
    private List<ForgePolicy> policies = new ArrayList<>();

    @Builder.Default
    private List<ForgeTask> handlers = new ArrayList<>();
}
