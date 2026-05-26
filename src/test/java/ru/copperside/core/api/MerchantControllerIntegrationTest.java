package ru.copperside.core.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void merchantsEndpointSupportsPaginationSearchAndSorting() throws Exception {
        mockMvc.perform(get("/api/v1/merchants")
                        .param("limit", "1")
                        .param("offset", "0")
                        .param("search", "alpha")
                        .param("sortBy", "name")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(1))
                .andExpect(jsonPath("$.meta.limit").value(1))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void internalAdminMerchantsEndpointSupportsPaginationSearchAndSorting() throws Exception {
        mockMvc.perform(get("/internal/v1/admin/merchants")
                        .param("limit", "1")
                        .param("offset", "0")
                        .param("search", "alpha")
                        .param("sortBy", "name")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(1))
                .andExpect(jsonPath("$.meta.limit").value(1));
    }

    @Test
    void internalAdminMerchantDetailsEndpointReturnsMerchant() throws Exception {
        mockMvc.perform(get("/internal/v1/admin/merchants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mercId").value(1))
                .andExpect(jsonPath("$.data.name").value("Alpha Shop"));
    }

    @Test
    void activeConfigurationLineEndpointSupportsFilters() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/configurations/active-line")
                        .param("limit", "10")
                        .param("offset", "0")
                        .param("search", "2222222222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(2))
                .andExpect(jsonPath("$.data[0].configuration.INN").value("2222222222"));
    }

    @Test
    void configurationsEndpointSupportsAtAndSearch() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/1/configurations")
                        .param("at", "2021-01-01T00:00:00Z")
                        .param("search", "mode")
                        .param("sortBy", "parameterName")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].parameterName").value("MODE"))
                .andExpect(jsonPath("$.data[0].parameterValue").value("B"));
    }

    @Test
    void configurationHistoryEndpointSupportsPaginationAndSorting() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/1/configuration-history")
                        .param("limit", "1")
                        .param("offset", "0")
                        .param("sortBy", "dateBegin")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
