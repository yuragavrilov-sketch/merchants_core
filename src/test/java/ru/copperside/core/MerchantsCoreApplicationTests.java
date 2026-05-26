package ru.copperside.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MerchantsCoreApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void defaultServerPortMatchesAdminBffContract() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8082");
    }

    @Test
    void applicationPackageUsesCoppersideRoot() {
        assertThat(MerchantsCoreApplication.class.getPackageName()).isEqualTo("ru.copperside.core");
    }
}
