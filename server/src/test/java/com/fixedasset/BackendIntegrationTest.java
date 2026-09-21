package com.fixedasset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanLoginAndReadCoreApis() throws Exception {
        String token = login("admin", "123456");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));

        mockMvc.perform(get("/api/assets?page=1&size=5").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.records.length()").value(5));

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    void employeeCannotCallManagementApi() throws Exception {
        String token = login("employee", "123456");

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"测试部门","code":"TEST","status":"ACTIVE"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void loanAndReturnFlowKeepsAssetStatusConsistent() throws Exception {
        String employeeToken = login("employee", "123456");
        String adminToken = login("admin", "123456");

        String applyResponse = mockMvc.perform(post("/api/loans")
                        .header("Authorization", bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetId":3,"remark":"集成测试领用"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long loanId = objectMapper.readTree(applyResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/loans/{id}/approve", loanId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/assets/3").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_USE"))
                .andExpect(jsonPath("$.data.ownerId").value(1));

        mockMvc.perform(post("/api/loans/{id}/request-return", loanId)
                        .header("Authorization", bearer(employeeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURN_PENDING"));

        mockMvc.perform(post("/api/loans/{id}/confirm-return", loanId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));

        mockMvc.perform(get("/api/assets/3").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IDLE"));
    }

    @Test
    void transferUpdatesDepartmentAndOwner() throws Exception {
        String adminToken = login("admin", "123456");

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetId":1,
                                  "toDepartmentId":3,
                                  "toOwnerId":3,
                                  "reason":"集成测试调拨"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/assets/1").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").value(3))
                .andExpect(jsonPath("$.data.ownerId").value(3));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginPayload(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        String token = root.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record LoginPayload(String username, String password) {
    }
}
