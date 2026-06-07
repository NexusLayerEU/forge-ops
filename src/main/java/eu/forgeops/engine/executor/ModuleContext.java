package eu.forgeops.engine.executor;

import com.jcraft.jsch.Session;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleContext {

    private UUID nodeId;
    private String nodeName;
    private String hostname;
    private int port;
    private String osType;
    private Session sshSession;

    private Map<String, Object> nodeVariables;
    private Map<String, Object> groupVariables;

    private UUID runId;
    private UUID taskId;
    private String forgeVersion;
    private String forgeName;

    private OutputHandler outputHandler;

    /** Merged variables: group vars overridden by node vars. */
    public Map<String, Object> getVars() {
        Map<String, Object> merged = new java.util.HashMap<>();
        if (groupVariables != null) merged.putAll(groupVariables);
        if (nodeVariables != null) merged.putAll(nodeVariables);
        return merged;
    }
}
