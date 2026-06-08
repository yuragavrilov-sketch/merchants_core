package ru.copperside.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupDiagnosticsTest {

    @Test
    void logsSafeConfigurationSummaryWithoutRawSecrets(CapturedOutput output) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "merchants-core")
                .withProperty("spring.profiles.active", "local")
                .withProperty(
                        "spring.config.import",
                        "optional:configserver:http://pay-payconfig-server:8080,optional:vault://"
                )
                .withProperty("spring.datasource.url", "jdbc:oracle:thin:@localhost:1521/XEPDB1")
                .withProperty("spring.datasource.hikari.maximum-pool-size", "7");

        InternalAdminSecurityProperties properties =
                new InternalAdminSecurityProperties("super-secret-token", Map.of(), "X-Internal-Admin-Key");

        new StartupDiagnostics(environment, properties).logConfiguration();

        assertThat(output)
                .contains("merchants-core")
                .contains("local")
                .contains("configserver")
                .contains("vault")
                .contains("jdbc:oracle:thin:@localhost:1521/XEPDB1")
                .contains("7")
                .contains("supe...oken")
                .doesNotContain("super-secret-token");
    }
}
