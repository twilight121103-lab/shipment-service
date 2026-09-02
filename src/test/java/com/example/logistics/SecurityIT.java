package com.example.logistics;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC authorization tests against the real Keycloak role model.
 */
class SecurityIT extends AbstractIntegrationTest {

    @Test
    void listShipments_requiresOperatorOrAdmin() throws Exception {
        // A plain LOGISTICS_USER must NOT be able to list all shipments.
        mockMvc.perform(get("/api/v1/shipments")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.containsString("forbidden")));
    }

    @Test
    void adminCanListShipments() throws Exception {
        mockMvc.perform(get("/api/v1/shipments")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void operatorCanListShipments() throws Exception {
        mockMvc.perform(get("/api/v1/shipments")
                        .header("Authorization", "Bearer " + operatorToken()))
                .andExpect(status().isOk());
    }
}
