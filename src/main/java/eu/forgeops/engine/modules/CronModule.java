package eu.forgeops.engine.modules;

import eu.forgeops.engine.executor.ModuleContext;
import eu.forgeops.engine.executor.ModuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CronModule extends AbstractSshModule {

    @Override
    public String getName() { return "cron"; }

    @Override
    public List<String> getSupportedOs() { return List.of("linux"); }

    @Override
    public ModuleResult execute(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "forgeops-cron");
        String job = (String) params.get("job");
        String state = (String) params.getOrDefault("state", "present");
        String minute = (String) params.getOrDefault("minute", "*");
        String hour = (String) params.getOrDefault("hour", "*");
        String day = (String) params.getOrDefault("day", "*");
        String month = (String) params.getOrDefault("month", "*");
        String weekday = (String) params.getOrDefault("weekday", "*");
        String user = (String) params.getOrDefault("user", "root");

        String marker = "# FORGEOPS:" + name;
        String cmd;
        if ("absent".equals(state)) {
            cmd = "crontab -l -u " + user + " 2>/dev/null | grep -v '" + marker + "' | grep -v '" +
                  (job != null ? job.replace("'", "'\\''") : "") + "' | crontab -u " + user + " -";
        } else {
            String cronLine = minute + " " + hour + " " + day + " " + month + " " + weekday + " " + job + " " + marker;
            // Remove existing entry with same name then add new
            cmd = "(crontab -l -u " + user + " 2>/dev/null | grep -v '" + marker + "'; echo '" + cronLine + "') | crontab -u " + user + " -";
        }

        return executeSsh(ctx, cmd, 15);
    }

    @Override
    public Map<String, Object> checkState(ModuleContext ctx, Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "forgeops-cron");
        String user = (String) params.getOrDefault("user", "root");
        String result = checkSsh(ctx, "crontab -l -u " + user + " 2>/dev/null | grep 'FORGEOPS:" + name + "' || echo absent");
        return Map.of("present", !result.equals("absent"));
    }
}
