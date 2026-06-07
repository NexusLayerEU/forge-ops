package eu.forgeops.engine.modules;

import com.jcraft.jsch.ChannelSftp;
import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class CopyModule extends AbstractSshModule {

    @Override
    public String getName() { return "copy"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String content = (String) params.get("content");
        String dest = (String) params.getOrDefault("dest", "");
        String mode = (String) params.getOrDefault("mode", "0644");
        String owner = (String) params.get("owner");
        String group = (String) params.get("group");

        if (ctx.getSshSession() == null || !ctx.getSshSession().isConnected()) {
            return ModuleResult.failed("SSH session not connected", 1);
        }

        try {
            ChannelSftp sftp = (ChannelSftp) ctx.getSshSession().openChannel("sftp");
            sftp.connect(10000);

            byte[] bytes = content != null ? content.getBytes() : new byte[0];
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                sftp.put(is, dest);
            }
            sftp.disconnect();

            StringBuilder postCmd = new StringBuilder("chmod ").append(mode).append(" ").append(dest);
            if (owner != null || group != null) {
                String ownerGroup = (owner != null ? owner : "") + (group != null ? ":" + group : "");
                postCmd.append(" && chown ").append(ownerGroup).append(" ").append(dest);
            }
            return executeSsh(ctx, postCmd.toString(), 15);
        } catch (Exception e) {
            return ModuleResult.failed("SFTP copy failed: " + e.getMessage(), 1);
        }
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String dest = (String) params.getOrDefault("dest", "");
        String result = checkSsh(ctx, "test -f " + dest + " && sha256sum " + dest + " | awk '{print $1}' || echo absent");
        return Map.of("exists", !result.equals("absent"), "checksum", result.equals("absent") ? "" : result);
    }
}
