package ru.copperside.core.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет per-caller модель: ключ, объявленный в {@code accepted-callers}, принимается даже без
 * legacy {@code api-key}; чужой ключ отклоняется.
 */
@SpringBootTest(properties = "merchants-core.internal-admin.accepted-callers.payadmin-bff=bff-caller-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalAdminAcceptedCallersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsKeyOfDeclaredCaller() throws Exception {
        mockMvc.perform(get("/internal/v1/admin/merchants")
                        .header("X-Internal-Admin-Key", "bff-caller-secret")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(1));
    }

    @Test
    void rejectsUnknownKey() throws Exception {
        mockMvc.perform(get("/internal/v1/admin/merchants")
                        .header("X-Internal-Admin-Key", "not-the-key")
                        .param("limit", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
