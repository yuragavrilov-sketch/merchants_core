package ru.copperside.core.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActiveConfigurationClockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void activeConfigurationLineUsesApplicationUtcClock() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/configurations/active-line")
                        .param("limit", "1")
                        .param("search", "1111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configuration.MODE").value("A"));
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2005-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
