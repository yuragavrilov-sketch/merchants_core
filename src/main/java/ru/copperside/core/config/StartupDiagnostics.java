package ru.copperside.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class StartupDiagnostics implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final Environment environment;
    private final InternalAdminSecurityProperties internalAdminProperties;

    public StartupDiagnostics(Environment environment, InternalAdminSecurityProperties internalAdminProperties) {
        this.environment = environment;
        this.internalAdminProperties = internalAdminProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logConfiguration();
    }

    void logConfiguration() {
        log.info(
                "Startup configuration: application={}, profiles={}, configImports={}, vaultEnabled={}, "
                        + "datasourceUrl={}, datasourcePoolMax={}, internalAdminKey={}",
                property("spring.application.name", "unknown"),
                profiles(),
                configImports(),
                property("spring.cloud.vault.enabled", "true"),
                property("spring.datasource.url", "not-configured"),
                property("spring.datasource.hikari.maximum-pool-size", "default"),
                internalAdminKey()
        );
    }

    private String profiles() {
        String activeProperty = environment.getProperty("spring.profiles.active");
        if (activeProperty != null && !activeProperty.isBlank()) {
            return activeProperty;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return String.join(",", activeProfiles);
        }

        return String.join(",", environment.getDefaultProfiles());
    }

    private String configImports() {
        String directImport = environment.getProperty("spring.config.import");
        if (directImport != null && !directImport.isBlank()) {
            return directImport;
        }

        String indexedImports = IntStream.range(0, 10)
                .mapToObj(index -> environment.getProperty("spring.config.import[" + index + "]"))
                .takeWhile(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(","));

        return indexedImports.isBlank() ? "not-configured" : indexedImports;
    }

    private String property(String name, String defaultValue) {
        return environment.getProperty(name, defaultValue);
    }

    private String internalAdminKey() {
        if (!internalAdminProperties.enabled()) {
            return "disabled";
        }
        return mask(internalAdminProperties.apiKey());
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "disabled";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
