package eu.forgeops.engine.executor;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleResult {

    public enum Status { OK, CHANGED, FAILED, SKIPPED }

    private Status status;
    private int exitCode;
    private String message;

    @Builder.Default
    private Map<String, Object> output = new HashMap<>();

    public boolean isSuccess() {
        return status == Status.OK || status == Status.CHANGED;
    }

    public static ModuleResult ok() {
        return ModuleResult.builder().status(Status.OK).exitCode(0).build();
    }

    public static ModuleResult changed() {
        return ModuleResult.builder().status(Status.CHANGED).exitCode(0).build();
    }

    public static ModuleResult failed(String message, int exitCode) {
        return ModuleResult.builder().status(Status.FAILED).exitCode(exitCode).message(message).build();
    }

    public static ModuleResult skipped() {
        return ModuleResult.builder().status(Status.SKIPPED).exitCode(0).build();
    }
}
