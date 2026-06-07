package eu.forgeops.engine.parser;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ForgeSpecParser {

    private final YAMLMapper yamlMapper;

    public ForgeSpecDocument parse(String yamlContent) {
        try {
            return yamlMapper.readValue(yamlContent, ForgeSpecDocument.class);
        } catch (IOException e) {
            throw new ForgeSpecParseException("Invalid YAML: " + e.getMessage(), e);
        }
    }

    public static class ForgeSpecParseException extends RuntimeException {
        public ForgeSpecParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
