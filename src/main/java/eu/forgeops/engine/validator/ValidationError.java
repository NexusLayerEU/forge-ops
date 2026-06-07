package eu.forgeops.engine.validator;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationError {

    public enum Severity { ERROR, WARNING }

    private Severity severity;
    private String field;
    private String message;

    public static ValidationError error(String field, String message) {
        return new ValidationError(Severity.ERROR, field, message);
    }

    public static ValidationError warning(String field, String message) {
        return new ValidationError(Severity.WARNING, field, message);
    }
}
