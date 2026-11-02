package io.github.abinesh1006.docsassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocsAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocsAssistantApplication.class, args);
    }
}
