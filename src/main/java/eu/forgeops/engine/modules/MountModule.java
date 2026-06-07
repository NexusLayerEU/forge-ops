package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MountModule extends AbstractSshModule {

    @Override
    public String getName() { return "mount"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String path = (String) params.getOrDefault("path", "");
        String src = (String) params.getOrDefault("src", "");
        String fstype = (String) params.getOrDefault("fstype", "ext4");
        String opts = (String) params.getOrDefault("opts", "defaults");
        String state = (String) params.getOrDefault("state", "mounted");

        String cmd = switch (state) {
            case "mounted" -> "mkdir -p " + path +
                " && mount -t " + fstype + " -o " + opts + " " + src + " " + path +
                " && grep -q '" + src + "' /etc/fstab || echo '" + src + " " + path + " " + fstype + " " + opts + " 0 0' >> /etc/fstab";
            case "unmounted" -> "umount " + path + " 2>/dev/null || true";
            case "absent" -> "umount " + path + " 2>/dev/null; " +
                "sed -i '\\|^" + src.replace("/", "\\/") + " |d' /etc/fstab";
            default -> "echo 'Unknown mount state: " + state + "'; exit 1";
        };

        return executeSsh(ctx, cmd, 30);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String path = (String) params.getOrDefault("path", "");
        String result = checkSsh(ctx, "mountpoint -q " + path + " && echo mounted || echo unmounted");
        return Map.of("mounted", result.contains("mounted"));
    }
}
