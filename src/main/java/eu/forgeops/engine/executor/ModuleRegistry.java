package eu.forgeops.engine.executor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModuleRegistry {

    private final Map<String, ForgeModule> modules;

    public ModuleRegistry(List<ForgeModule> moduleList) {
        this.modules = moduleList.stream()
            .collect(Collectors.toMap(ForgeModule::getName, Function.identity()));
    }

    public ForgeModule get(String name) {
        return modules.get(name);
    }

    public boolean supports(String name) {
        return modules.containsKey(name);
    }

    public java.util.Set<String> names() {
        return modules.keySet();
    }
}
