package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserModule extends AbstractSshModule {

    @Override
    public String getName() { return "user"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "");
        String state = (String) params.getOrDefault("state", "present");
        String shell = (String) params.getOrDefault("shell", "/bin/bash");
        String home = (String) params.get("home");
        String groups = (String) params.get("groups");
        Boolean system = (Boolean) params.getOrDefault("system", false);
        Boolean createHome = (Boolean) params.getOrDefault("create_home", true);

        String cmd;
        if ("absent".equals(state)) {
            cmd = "userdel -r " + name + " 2>/dev/null || true";
        } else {
            StringBuilder userAdd = new StringBuilder("id " + name + " &>/dev/null && usermod");
            userAdd.append(" -s ").append(shell);
            if (home != null) userAdd.append(" -d ").append(home);
            if (groups != null) userAdd.append(" -aG ").append(groups);
            userAdd.append(" ").append(name);
            userAdd.append(" || useradd");
            if (Boolean.TRUE.equals(system)) userAdd.append(" --system");
            if (!Boolean.TRUE.equals(createHome)) userAdd.append(" -M");
            else userAdd.append(" -m");
            userAdd.append(" -s ").append(shell);
            if (home != null) userAdd.append(" -d ").append(home);
            if (groups != null) userAdd.append(" -G ").append(groups);
            userAdd.append(" ").append(name);
            cmd = userAdd.toString();
        }

        return executeSsh(ctx, cmd, 30);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "");
        String result = checkSsh(ctx, "id " + name + " 2>/dev/null && echo exists || echo absent");
        return Map.of("exists", result.contains("exists"));
    }
}
