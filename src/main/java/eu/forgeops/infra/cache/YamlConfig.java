package eu.forgeops.infra.cache;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YamlConfig {

    @Bean
    public YAMLMapper yamlMapper() {
        return new YAMLMapper();
    }
}
