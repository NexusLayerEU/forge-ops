package eu.forgeops.engine.modules;

import com.jcraft.jsch.ChannelExec;
import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import eu.forgeops.engine.executor.ForgeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractSshModule implements ForgeModule {

    protected ModuleResult executeSsh(ModuleContext ctx, String command, int timeoutSeconds) {
        if (ctx.getSshSession() == null || !ctx.getSshSession().isConnected()) {
            return ModuleResult.failed("SSH session not connected", 1);
        }
        try {
            ChannelExec channel = (ChannelExec) ctx.getSshSession().openChannel("exec");
            channel.setCommand(command);
            channel.setPty(false);

            var stdout = channel.getInputStream();
            var stderr = channel.getErrStream();
            channel.connect(timeoutSeconds * 1000);

            AtomicInteger lineNo = new AtomicInteger(0);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            String line;
            StringBuilder outputBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
                if (ctx.getOutputHandler() != null) {
                    ctx.getOutputHandler().emit(ctx.getRunId(), ctx.getTaskId(), ctx.getNodeId(),
                        ctx.getNodeName(), lineNo.getAndIncrement(), "info", line);
                }
            }

            // Read stderr
            BufferedReader errReader = new BufferedReader(new InputStreamReader(stderr));
            while ((line = errReader.readLine()) != null) {
                if (ctx.getOutputHandler() != null) {
                    ctx.getOutputHandler().emit(ctx.getRunId(), ctx.getTaskId(), ctx.getNodeId(),
                        ctx.getNodeName(), lineNo.getAndIncrement(), "warn", line);
                }
            }

            int exitCode = channel.getExitStatus();
            channel.disconnect();

            if (exitCode == 0) {
                String output = outputBuilder.toString().trim();
                return buildResult(output, exitCode);
            } else {
                return ModuleResult.failed("Command exited with code " + exitCode, exitCode);
            }
        } catch (Exception e) {
            log.error("SSH execution failed", e);
            return ModuleResult.failed("SSH error: " + e.getMessage(), 1);
        }
    }

    protected String checkSsh(ModuleContext ctx, String command) {
        if (ctx.getSshSession() == null || !ctx.getSshSession().isConnected()) {
            return "";
        }
        try {
            ChannelExec channel = (ChannelExec) ctx.getSshSession().openChannel("exec");
            channel.setCommand(command);
            channel.setPty(false);
            var stdout = channel.getInputStream();
            channel.connect(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            channel.disconnect();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    protected ModuleResult buildResult(String output, int exitCode) {
        if (exitCode == 0) {
            return ModuleResult.ok();
        }
        return ModuleResult.failed(output, exitCode);
    }
}
