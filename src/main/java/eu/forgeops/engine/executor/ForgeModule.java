package eu.forgeops.engine.executor;

import java.util.List;
import java.util.Map;

/**
 * Interface all built-in ForgeOps modules must implement.
 */
public interface ForgeModule {

    /** Module name as referenced in ForgeSpec (e.g., "package") */
    String getName();

    /** OS types this module supports */
    List<String> getSupportedOs();

    /** Execute the module task — run on node via SSH/WinRM */
    ModuleResult execute(ModuleContext context, Map<String, Object> params);

    /** Check current state (for drift detection) — read-only probe */
    Map<String, Object> checkState(ModuleContext context, Map<String, Object> params);
}
