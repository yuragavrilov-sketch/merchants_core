package ru.copperside.core.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "merchants-core.internal-admin.api-key=test-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalAdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void internalAdminEndpointRequiresApiKeyWhenConfigured() throws Exception {
        mockMvc.perform(get("/internal/v1/admin/merchants").param("limit", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().string(RequestIdFilter.HEADER, not(emptyString())))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void internalAdminEndpointAcceptsConfiguredApiKey() throws Exception {
        mockMvc.perform(get("/internal/v1/admin/merchants")
                        .header("X-Internal-Admin-Key", "test-secret")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(1));
    }

    @Test
    void publicApiDoesNotRequireInternalApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/merchants").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(1));
    }
}
