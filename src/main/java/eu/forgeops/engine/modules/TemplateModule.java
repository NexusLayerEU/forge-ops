package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateModule extends AbstractSshModule {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    @Override
    public String getName() { return "template"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String src = (String) params.getOrDefault("src", "");
        String dest = (String) params.getOrDefault("dest", "");
        String owner = (String) params.get("owner");
        String group = (String) params.get("group");
        String mode = (String) params.getOrDefault("mode", "0644");

        String rendered = render(src, ctx.getVars());

        // Write rendered content to remote file
        String escaped = rendered.replace("\\", "\\\\").replace("$", "\\$").replace("`", "\\`");
        String writeCmd = "cat > " + dest + " << 'FORGE_TMPL_EOF'\n" + rendered + "\nFORGE_TMPL_EOF";
        writeCmd += " && chmod " + mode + " " + dest;
        if (owner != null || group != null) {
            String ownerGroup = (owner != null ? owner : "") + (group != null ? ":" + group : "");
            writeCmd += " && chown " + ownerGroup + " " + dest;
        }

        return executeSsh(ctx, writeCmd, 30);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String dest = (String) params.getOrDefault("dest", "");
        String exists = checkSsh(ctx, "test -f " + dest + " && echo exists || echo absent");
        return Map.of("exists", exists.contains("exists"));
    }

    private String render(String template, Map<String, Object> vars) {
        if (vars == null) return template;
        Matcher m = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            Object val = vars.get(key);
            m.appendReplacement(sb, val != null ? Matcher.quoteReplacement(val.toString()) : m.group(0));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
