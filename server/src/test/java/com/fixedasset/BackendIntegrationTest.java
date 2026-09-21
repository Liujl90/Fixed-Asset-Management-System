package com.fixedasset;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixedasset.asset.excel.AssetExcelRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private Scheduler scheduler;

    @Test
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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

    @Test
    @Order(5)
    void quartzJobsAndDepreciationWork() throws Exception {
        String adminToken = login("admin", "123456");

        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
        assertThat(jobKeys)
                .extracting(JobKey::getName)
                .contains("depreciationJob", "maintenanceDueCheckJob");

        mockMvc.perform(post("/api/operations/jobs/depreciation")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"2026-09"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").isNumber());

        mockMvc.perform(get("/api/operations/depreciations?page=1&size=10")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber());

        mockMvc.perform(post("/api/operations/jobs/maintenance-check")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").isNumber());
    }

    @Test
    @Order(6)
    void dashboardUsesCacheAndExcelCanExport() throws Exception {
        String adminToken = login("admin", "123456");

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        assertThat(cacheManager.getCache("dashboardSummary")).isNotNull();
        assertThat(cacheManager.getCache("dashboardSummary").get("summary")).isNotNull();

        mockMvc.perform(get("/api/assets/export").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .contains("spreadsheetml"));
    }

    @Test
    @Order(7)
    void excelImportCreatesAsset() throws Exception {
        String adminToken = login("admin", "123456");
        AssetExcelRow row = new AssetExcelRow();
        row.setAssetNo("FA-IMPORT-001");
        row.setName("Excel 导入测试资产");
        row.setCategoryId(4L);
        row.setBrandModel("Import Model");
        row.setPurchaseDate("2026-09-01");
        row.setOriginalValue(new BigDecimal("1000.00"));
        row.setUsefulLife(5);
        row.setDepartmentId(1L);
        row.setStatus("IDLE");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, AssetExcelRow.class).sheet("固定资产").doWrite(java.util.List.of(row));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "assets.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                output.toByteArray());

        mockMvc.perform(multipart("/api/assets/import")
                        .file(file)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(1));

        mockMvc.perform(get("/api/assets?page=1&size=20&keyword=FA-IMPORT-001")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @Order(8)
    void purchaseLifecycleWorks() throws Exception {
        String adminToken = login("admin", "123456");

        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"测试供应商",
                                  "code":"SUP-TEST",
                                  "contactName":"测试联系人",
                                  "status":"ACTIVE"
                                }
                                """))
                .andExpect(status().isOk());

        String createResponse = mockMvc.perform(post("/api/purchases")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderNo":"PO-TEST-001",
                                  "supplierId":1,
                                  "applicantId":4,
                                  "orderDate":"2026-09-22",
                                  "expectedDate":"2026-10-01",
                                  "remark":"集成测试采购",
                                  "items":[
                                    {
                                      "assetName":"测试采购资产",
                                      "categoryId":4,
                                      "quantity":2,
                                      "unitPrice":1000,
                                      "remark":"测试"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long purchaseId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/purchases/{id}/submit", purchaseId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/purchases/{id}/approve", purchaseId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @Order(9)
    void inboundConfirmCreatesAssets() throws Exception {
        String adminToken = login("admin", "123456");

        mockMvc.perform(post("/api/inbounds/1/confirm")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAssets").value(3))
                .andExpect(jsonPath("$.data.order.status").value("CONFIRMED"));
    }

    @Test
    @Order(10)
    void maintenanceRecordRestoresAssetStatus() throws Exception {
        String adminToken = login("admin", "123456");

        String response = mockMvc.perform(post("/api/operations/maintenance-records")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maintenanceNo":"MT-TEST-001",
                                  "assetId":6,
                                  "maintenanceType":"REPAIR",
                                  "description":"测试维修",
                                  "cost":100,
                                  "startDate":"2026-09-22",
                                  "operatorId":2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long recordId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(post("/api/operations/maintenance-records/{id}/start", recordId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        mockMvc.perform(get("/api/assets/6").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));

        mockMvc.perform(post("/api/operations/maintenance-records/{id}/complete", recordId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result":"维修完成",
                                  "cost":150,
                                  "endDate":"2026-09-22"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/assets/6").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IDLE"));
    }

    @Test
    @Order(11)
    void inventoryAndScrapLifecycleWork() throws Exception {
        String adminToken = login("admin", "123456");

        String inventoryResponse = mockMvc.perform(post("/api/operations/inventory-checks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checkNo":"IC-TEST-001",
                                  "checkName":"测试盘点",
                                  "departmentId":2,
                                  "checkDate":"2026-09-22",
                                  "operatorId":2,
                                  "remark":"集成测试"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long inventoryId = objectMapper.readTree(inventoryResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/operations/inventory-checks/{id}/complete", inventoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        String scrapResponse = mockMvc.perform(post("/api/operations/scraps")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scrapNo":"SC-TEST-001",
                                  "assetId":3,
                                  "reason":"测试报废",
                                  "applicantId":4,
                                  "remark":"集成测试"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long scrapId = objectMapper.readTree(scrapResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/operations/scraps/{id}/approve", scrapId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/operations/scraps/{id}/complete", scrapId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "disposalMethod":"环保回收",
                                  "disposalAmount":50,
                                  "remark":"处置完成"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/assets/3").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCRAPPED"));
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
