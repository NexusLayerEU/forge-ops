package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WaitForModule extends AbstractSshModule {

    @Override
    public String getName() { return "wait_for"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux", "windows"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String host = (String) params.getOrDefault("host", "localhost");
        Object portObj = params.get("port");
        String path = (String) params.get("path");
        int timeout = ((Number) params.getOrDefault("timeout", 30)).intValue();
        int delay = ((Number) params.getOrDefault("delay", 2)).intValue();
        String state = (String) params.getOrDefault("state", "started");

        String checkCmd;
        if (path != null) {
            checkCmd = "test -e " + path + " && echo ok || echo absent";
        } else if (portObj != null) {
            int port = ((Number) portObj).intValue();
            if ("stopped".equals(state)) {
                checkCmd = "! (echo > /dev/tcp/" + host + "/" + port + ") 2>/dev/null && echo ok || echo open";
            } else {
                checkCmd = "(echo > /dev/tcp/" + host + "/" + port + ") 2>/dev/null && echo ok || echo closed";
            }
        } else {
            return ModuleResult.failed("wait_for requires either 'port' or 'path'", 1);
        }

        // Poll with timeout using a shell loop
        String pollCmd = "TIMEOUT=" + timeout + "; DELAY=" + delay + "; ELAPSED=0; " +
            "while [ $ELAPSED -lt $TIMEOUT ]; do " +
            "  RESULT=$(" + checkCmd + "); " +
            "  if [ \"$RESULT\" = 'ok' ]; then echo 'Condition met'; exit 0; fi; " +
            "  sleep $DELAY; ELAPSED=$((ELAPSED + DELAY)); " +
            "done; echo 'Timeout waiting for condition'; exit 1";

        return executeSsh(ctx, pollCmd, timeout + 10);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        return Map.of();
    }
}
