package eu.forgeops.engine.parser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForgeTarget {

    @Builder.Default
    private List<String> groups = new ArrayList<>();

    @Builder.Default
    private List<String> nodes = new ArrayList<>();
}
