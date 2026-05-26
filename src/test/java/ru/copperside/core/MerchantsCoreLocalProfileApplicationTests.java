package ru.copperside.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:merchants_core_local;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always"
})
@ActiveProfiles("local")
class MerchantsCoreLocalProfileApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoadsWithLocalProfileWithoutExternalConfigSources() {
        assertThat(environment.getActiveProfiles()).contains("local");
        assertThat(environment.getProperty("pay.environment")).isEqualTo("local");
        assertThat(environment.getProperty("spring.cloud.config.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("spring.cloud.vault.enabled")).isEqualTo("false");
    }
}
