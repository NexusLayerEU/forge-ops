package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PackageModule extends AbstractSshModule {

    @Override
    public String getName() { return "package"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux", "windows"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "");
        String state = (String) params.getOrDefault("state", "present");
        String version = (String) params.getOrDefault("version", null);

        String pkgManager = detectPackageManager(ctx);
        String cmd = buildCommand(pkgManager, name, state, version);

        if (cmd == null) return ModuleResult.failed("Cannot determine package manager", 1);
        return executeSsh(ctx, cmd, 120);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "");
        String result = checkSsh(ctx, "dpkg -s " + name + " 2>/dev/null | grep -i status || rpm -q " + name + " 2>/dev/null || echo 'not-installed'");
        boolean installed = !result.contains("not installed") && !result.contains("not-installed") && !result.isBlank();
        return Map.of("installed", installed, "name", name);
    }

    private String detectPackageManager(ModuleContext ctx) {
        String result = checkSsh(ctx, "which apt-get 2>/dev/null && echo apt || which yum 2>/dev/null && echo yum || which dnf 2>/dev/null && echo dnf || echo unknown");
        if (result.contains("apt")) return "apt";
        if (result.contains("yum")) return "yum";
        if (result.contains("dnf")) return "dnf";
        return "apt"; // default
    }

    private String buildCommand(String pkgManager, String name, String state, String version) {
        String pkg = version != null ? name + "=" + version : name;
        return switch (pkgManager) {
            case "apt" -> switch (state) {
                case "present" -> "DEBIAN_FRONTEND=noninteractive apt-get install -y " + pkg;
                case "absent"  -> "apt-get remove -y " + name;
                case "latest"  -> "apt-get install -y --only-upgrade " + name;
                default -> null;
            };
            case "yum", "dnf" -> switch (state) {
                case "present" -> pkgManager + " install -y " + pkg;
                case "absent"  -> pkgManager + " remove -y " + name;
                case "latest"  -> pkgManager + " update -y " + name;
                default -> null;
            };
            default -> null;
        };
    }
}
