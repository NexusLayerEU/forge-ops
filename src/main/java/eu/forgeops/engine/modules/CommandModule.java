package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CommandModule extends AbstractSshModule {

    @Override
    public String getName() { return "command"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux", "windows"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String cmd = (String) params.getOrDefault("cmd", "");
        String creates = (String) params.get("creates");
        String removes = (String) params.get("removes");
        String chdir = (String) params.get("chdir");

        if (creates != null) {
            String check = checkSsh(ctx, "test -e " + creates + " && echo exists || echo absent");
            if (check.contains("exists")) return ModuleResult.skipped();
        }
        if (removes != null) {
            String check = checkSsh(ctx, "test -e " + removes + " && echo exists || echo absent");
            if (check.contains("absent")) return ModuleResult.skipped();
        }

        String fullCmd = chdir != null ? "cd " + chdir + " && " + cmd : cmd;
        return executeSsh(ctx, fullCmd, 60);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        return Map.of();
    }
}
