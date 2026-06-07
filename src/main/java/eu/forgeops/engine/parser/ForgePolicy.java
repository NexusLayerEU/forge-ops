package eu.forgeops.engine.parser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForgePolicy {

    private String name;
    private String resource;

    @Builder.Default
    private Map<String, Object> params = new LinkedHashMap<>();

    @JsonProperty("on_drift")
    @Builder.Default
    private String onDrift = "alert";

    @JsonProperty("check_interval")
    @Builder.Default
    private int checkInterval = 3600;

    private ForgeTarget targets;
}
