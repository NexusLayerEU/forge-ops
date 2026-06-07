package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FileModule extends AbstractSshModule {

    @Override
    public String getName() { return "file"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String path = (String) params.getOrDefault("path", "");
        String state = (String) params.getOrDefault("state", "present");
        String content = (String) params.get("content");
        String owner = (String) params.get("owner");
        String group = (String) params.get("group");
        String mode = (String) params.get("mode");

        StringBuilder cmd = new StringBuilder();
        switch (state) {
            case "present" -> {
                if (content != null) {
                    // Write content using heredoc
                    String escaped = content.replace("'", "'\\''");
                    cmd.append("cat > ").append(path).append(" << 'FORGE_EOF'\n").append(content).append("\nFORGE_EOF");
                } else {
                    cmd.append("touch ").append(path);
                }
            }
            case "absent"    -> cmd.append("rm -f ").append(path);
            case "directory" -> cmd.append("mkdir -p ").append(path);
            case "link"      -> {
                String src = (String) params.getOrDefault("src", "");
                cmd.append("ln -sf ").append(src).append(" ").append(path);
            }
        }

        if (!cmd.isEmpty() && (owner != null || group != null)) {
            String ownerGroup = (owner != null ? owner : "") + (group != null ? ":" + group : "");
            cmd.append(" && chown ").append(ownerGroup).append(" ").append(path);
        }
        if (!cmd.isEmpty() && mode != null) {
            cmd.append(" && chmod ").append(mode).append(" ").append(path);
        }

        return executeSsh(ctx, cmd.toString(), 30);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String path = (String) params.getOrDefault("path", "");
        String result = checkSsh(ctx, "stat -c '%F %a %U %G' " + path + " 2>/dev/null || echo 'absent'");
        boolean exists = !result.equals("absent");
        Map<String, Object> state = new java.util.HashMap<>();
        state.put("exists", exists);
        if (exists) {
            String[] parts = result.split(" ");
            if (parts.length >= 2) state.put("mode", parts[1]);
            if (parts.length >= 3) state.put("owner", parts[2]);
            if (parts.length >= 4) state.put("group", parts[3]);
        }
        return state;
    }
}
