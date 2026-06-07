package eu.forgeops.engine.validator;

import eu.forgeops.engine.parser.ForgePolicy;
import eu.forgeops.engine.parser.ForgeSpecDocument;
import eu.forgeops.engine.parser.ForgeTask;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates a parsed ForgeSpec against the 10 rules in docs/04_forgespec_dsl.md.
 */
@Component
public class ForgeSpecValidator {

    private static final Set<String> SUPPORTED_VERSIONS = Set.of("1.0");
    private static final Set<String> KNOWN_MODULES = Set.of(
        "package", "service", "file", "template", "command",
        "user", "copy", "cron", "mount", "wait_for"
    );
    private static final Set<String> VALID_MODES = Set.of("policy", "playbook", "mixed");
    private static final Set<String> VALID_ON_DRIFT = Set.of("remediate", "alert", "ignore");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+(?:\\.\\w+)*)\\s*\\}\\}");
    private static final Set<String> RUNTIME_ONLY_VARS = Set.of("run_id");

    public ValidationResult validate(ForgeSpecDocument doc) {
        ValidationResult result = new ValidationResult();

        // Rule 1: forgespec version field present and supported
        if (doc.getForgespecVersion() == null || doc.getForgespecVersion().isBlank()) {
            result.addError("forgespec", "forgespec version field is required");
        } else if (!SUPPORTED_VERSIONS.contains(doc.getForgespecVersion())) {
            result.addError("forgespec", "Unsupported forgespec version: " + doc.getForgespecVersion() +
                ". Supported: " + SUPPORTED_VERSIONS);
        }

        // Rule 2: name is required
        if (doc.getName() == null || doc.getName().isBlank()) {
            result.addError("name", "name is required");
        }

        // Validate mode
        if (doc.getMode() != null && !VALID_MODES.contains(doc.getMode())) {
            result.addError("mode", "mode must be one of: " + VALID_MODES);
        }

        // Collect handler names for Rule 6
        Set<String> handlerNames = new HashSet<>();
        if (doc.getHandlers() != null) {
            for (ForgeTask handler : doc.getHandlers()) {
                if (handler.getName() != null) {
                    handlerNames.add(handler.getName());
                }
            }
        }

        // Validate tasks
        if (doc.getTasks() != null) {
            for (int i = 0; i < doc.getTasks().size(); i++) {
                validateTask(doc.getTasks().get(i), i, handlerNames, result);
            }
        }

        // Validate handlers
        if (doc.getHandlers() != null) {
            for (int i = 0; i < doc.getHandlers().size(); i++) {
                validateTask(doc.getHandlers().get(i), i, Collections.emptySet(), result);
            }
        }

        // Validate policies
        if (doc.getPolicies() != null) {
            for (ForgePolicy policy : doc.getPolicies()) {
                validatePolicy(policy, result);
            }
        }

        return result;
    }

    private void validateTask(ForgeTask task, int index, Set<String> handlerNames, ValidationResult result) {
        String prefix = "tasks[" + index + "]";

        // Rule 3: All module values reference known built-in modules
        if (task.getModule() == null || task.getModule().isBlank()) {
            result.addError(prefix + ".module", "module is required");
        } else if (!KNOWN_MODULES.contains(task.getModule())) {
            result.addError(prefix + ".module", "Unknown module: '" + task.getModule() +
                "'. Known modules: " + KNOWN_MODULES);
        }

        if (task.getName() == null || task.getName().isBlank()) {
            result.addError(prefix + ".name", "task name is required");
        }

        // Rule 6: notify references must resolve to handler names
        if (task.getNotify() != null && !handlerNames.isEmpty()) {
            for (String notifyName : task.getNotify()) {
                if (!handlerNames.contains(notifyName)) {
                    result.addError(prefix + ".notify", "Handler '" + notifyName + "' not found in handlers block");
                }
            }
        }

        // Rule 7: when expressions must be syntactically valid
        if (task.getWhen() != null && !task.getWhen().isBlank()) {
            validateWhenExpression(task.getWhen(), prefix + ".when", result);
        }

        // Rule 8: loop must be a list; loop_var must be a valid identifier
        if (task.getLoop() != null && !task.getLoop().isEmpty()) {
            if (task.getLoopVar() != null && !task.getLoopVar().isBlank()) {
                if (!IDENTIFIER_PATTERN.matcher(task.getLoopVar()).matches()) {
                    result.addError(prefix + ".loop_var", "loop_var must be a valid identifier");
                }
            }
        }

        // Rule 9: timeout must be a positive integer
        if (task.getTimeout() <= 0) {
            result.addError(prefix + ".timeout", "timeout must be a positive integer");
        }

        // Rule 4: flag runtime-only variables as warnings
        if (task.getParams() != null) {
            checkForRuntimeVars(task.getParams().toString(), prefix + ".params", result);
        }
    }

    private void validatePolicy(ForgePolicy policy, ValidationResult result) {
        String prefix = "policies[" + policy.getName() + "]";

        if (policy.getName() == null || policy.getName().isBlank()) {
            result.addError("policies", "Policy name is required");
        }

        if (policy.getResource() == null || policy.getResource().isBlank()) {
            result.addError(prefix + ".resource", "resource is required");
        } else if (!KNOWN_MODULES.contains(policy.getResource())) {
            result.addWarning(prefix + ".resource", "Unknown resource type: '" + policy.getResource() + "'");
        }

        // Rule 10: Policy on_drift must be one of the valid values
        if (policy.getOnDrift() != null && !VALID_ON_DRIFT.contains(policy.getOnDrift())) {
            result.addError(prefix + ".on_drift", "on_drift must be one of: " + VALID_ON_DRIFT);
        }

        if (policy.getCheckInterval() <= 0) {
            result.addError(prefix + ".check_interval", "check_interval must be a positive integer");
        }
    }

    private void validateWhenExpression(String when, String field, ValidationResult result) {
        // Basic syntax check: must contain comparison operators or be a variable reference
        if (!when.contains("{{") && !when.contains("==") && !when.contains("!=") &&
            !when.contains(">") && !when.contains("<") && !when.contains("in") &&
            !when.contains("and") && !when.contains("or") && !when.contains("not")) {
            result.addWarning(field, "when expression may not be a valid condition: " + when);
        }
    }

    private void checkForRuntimeVars(String content, String field, ValidationResult result) {
        var matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String varName = matcher.group(1).split("\\.")[0];
            if (RUNTIME_ONLY_VARS.contains(varName)) {
                result.addWarning(field, "Variable '{{ " + matcher.group(1) + " }}' is only available at runtime");
            }
        }
    }
}
