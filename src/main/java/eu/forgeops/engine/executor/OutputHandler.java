package eu.forgeops.engine.executor;

import java.util.UUID;

/**
 * Interface for streaming output lines from module execution.
 */
public interface OutputHandler {
    void emit(UUID runId, UUID taskId, UUID nodeId, String nodeName, int lineNo, String level, String message);
}
