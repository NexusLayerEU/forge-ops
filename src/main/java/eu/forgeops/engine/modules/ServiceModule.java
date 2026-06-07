package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ServiceModule extends AbstractSshModule {

    @Override
    public String getName() { return "service"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux", "windows"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "");
        String state = (String) params.getOrDefault("state", "started");
        Boolean enabled = (Boolean) params.get("enabled");

        StringBuilder cmds = new StringBuilder();
        String action = switch (state) {
            case "started"   -> "start";
            case "stopped"   -> "stop";
            case "restarted" -> "restart";
            case "reloaded"  -> "reload";
            default -> "start";
        };
        cmds.append("systemctl ").append(action).append(" ").append(name);

        if (enabled != null) {
            cmds.append(" && systemctl ").append(enabled ? "enable" : "disable").append(" ").append(name);
        }

        return executeSsh(ctx, cmds.toString(), 30);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "");
        String active = checkSsh(ctx, "systemctl is-active " + name + " 2>/dev/null || echo inactive");
        String enabled = checkSsh(ctx, "systemctl is-enabled " + name + " 2>/dev/null || echo disabled");
        return Map.of(
            "state", active.trim().equals("active") ? "running" : "stopped",
            "enabled", enabled.trim().equals("enabled")
        );
    }
}
