package eu.forgeops.engine.validator;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {

    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    @Builder.Default
    private List<ValidationError> warnings = new ArrayList<>();

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void addError(String field, String message) {
        errors.add(ValidationError.error(field, message));
    }

    public void addWarning(String field, String message) {
        warnings.add(ValidationError.warning(field, message));
    }

    public List<String> getErrorMessages() {
        return errors.stream().map(ValidationError::getMessage).collect(Collectors.toList());
    }
}
