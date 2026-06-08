package ru.copperside.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class InternalAdminSecurityPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsInternalAdminProperties() {
        contextRunner
                .withPropertyValues(
                        "merchants-core.internal-admin.header-name=X-Test-Key",
                        "merchants-core.internal-admin.api-key=secret-value"
                )
                .run(context -> {
                    InternalAdminSecurityProperties properties = context.getBean(InternalAdminSecurityProperties.class);

                    assertThat(properties.headerName()).isEqualTo("X-Test-Key");
                    assertThat(properties.enabled()).isTrue();
                });
    }

    @Test
    void bindsAcceptedCallersAndEnablesWithoutLegacyKey() {
        contextRunner
                .withPropertyValues(
                        "merchants-core.internal-admin.header-name=X-Internal-Admin-Key",
                        "merchants-core.internal-admin.accepted-callers.payadmin-bff=caller-secret"
                )
                .run(context -> {
                    InternalAdminSecurityProperties properties = context.getBean(InternalAdminSecurityProperties.class);

                    assertThat(properties.acceptedCallers()).containsEntry("payadmin-bff", "caller-secret");
                    assertThat(properties.hasLegacyKey()).isFalse();
                    assertThat(properties.enabled()).isTrue();
                });
    }

    @Test
    void disabledWhenOnlyBlankKeysConfigured() {
        contextRunner
                .withPropertyValues(
                        "merchants-core.internal-admin.header-name=X-Internal-Admin-Key",
                        "merchants-core.internal-admin.api-key=",
                        "merchants-core.internal-admin.accepted-callers.payadmin-bff="
                )
                .run(context -> {
                    InternalAdminSecurityProperties properties = context.getBean(InternalAdminSecurityProperties.class);

                    assertThat(properties.enabled()).isFalse();
                });
    }

    @Test
    void rejectsMissingHeaderName() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(InternalAdminSecurityProperties.class)
    static class PropertiesConfiguration {
    }
}
