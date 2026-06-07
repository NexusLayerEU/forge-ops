package eu.forgeops.engine.parser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForgeTask {

    private String name;
    private String module;

    @Builder.Default
    private Map<String, Object> params = new LinkedHashMap<>();

    private String when;

    @Builder.Default
    private List<Object> loop = new ArrayList<>();

    @JsonProperty("loop_var")
    private String loopVar;

    @Builder.Default
    private List<String> notify = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private int timeout = 30;

    @JsonProperty("ignore_errors")
    @Builder.Default
    private boolean ignoreErrors = false;

    @Builder.Default
    private boolean become = false;

    @JsonProperty("become_user")
    private String becomeUser;

    private String register;

    @Builder.Default
    private Map<String, Object> vars = new LinkedHashMap<>();
}
