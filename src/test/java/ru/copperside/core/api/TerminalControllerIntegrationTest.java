package ru.copperside.core.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TerminalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void terminalsListReturnsTotalAndMaskedPassword() throws Exception {
        mockMvc.perform(get("/api/v1/terminals").param("limit", "100").param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(4))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].mercId").value(1))
                .andExpect(jsonPath("$.data[0].hasPassword").value(true))
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].merchantName").value("Alpha Shop"));
    }

    @Test
    void terminalsListSupportsSearch() throws Exception {
        mockMvc.perform(get("/api/v1/terminals").param("search", "mir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].mps").value("MIR"))
                .andExpect(jsonPath("$.data[0].mercId").value(2));
    }

    @Test
    void merchantTerminalsAreScopedByMercId() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/1/terminals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].mercId").value(1))
                .andExpect(jsonPath("$.data[1].mercId").value(1));
    }

    @Test
    void terminalsListRejectsUnknownSortBy() throws Exception {
        mockMvc.perform(get("/api/v1/terminals").param("sortBy", "bogusField"))
                .andExpect(status().isBadRequest());
    }
}
