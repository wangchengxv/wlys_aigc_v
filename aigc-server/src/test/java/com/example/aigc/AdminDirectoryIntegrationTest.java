package com.example.aigc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AdminDirectoryIntegrationTest {
    private static final String TEST_ACCESS_TOKEN = "dev-local-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-admin-directory;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Test
    void shouldEnforcePermissionPointsAndAllowTemplateDownload() throws Exception {
        JsonNode studentListUsers = readResponseTree(mockMvc.perform(withAuth(get("/api/v1/admin/directory/users"),
                        "student-403", "学生", "org-school", null))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(studentListUsers.path("code").asInt()).isEqualTo(403);
        assertThat(studentListUsers.path("message").asText()).contains("权限");

        MvcResult teacherTemplate = mockMvc.perform(withAuth(get("/api/v1/admin/directory/users/import/template"),
                        "teacher-1", "教师", "org-school", null))
                .andExpect(status().isOk())
                .andReturn();
        String template = teacherTemplate.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(template).contains("username,displayName,role,password,orgUnitId,classroomId,enabled");
    }

    @Test
    void shouldSupportImportExportTaskQueryAndBatchStats() throws Exception {
        mockMvc.perform(withAuth(post("/api/v1/admin/directory/org-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "测试组织",
                                "type", "ORGANIZATION"
                        ))),
                "admin-1", "管理员", "org-school", null))
                .andExpect(status().isOk());

        String csv = String.join("\n",
                "username,displayName,role,password,orgUnitId,classroomId,enabled",
                "import-user-1,导入用户1,STUDENT,Pass@123,,,true",
                "bad-role-user,导入用户2,INVALID_ROLE,Pass@123,,,true"
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "accounts.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        JsonNode importResult = readSuccessData(mockMvc.perform(withAuth(multipart("/api/v1/admin/directory/users/import")
                                .file(file),
                        "admin-1", "管理员", "org-school", null))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(importResult.path("totalRows").asInt()).isEqualTo(2);
        assertThat(importResult.path("successRows").asInt()).isEqualTo(1);
        assertThat(importResult.path("failedRows").asInt()).isEqualTo(1);
        String taskId = importResult.path("taskId").asText();
        assertThat(taskId).isNotBlank();

        MvcResult exportResult = mockMvc.perform(withAuth(get("/api/v1/admin/directory/users/export"),
                        "admin-1", "管理员", "org-school", null))
                .andExpect(status().isOk())
                .andReturn();
        String exportCsv = exportResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(exportCsv).contains("userId,username,displayName");
        assertThat(exportCsv).contains("i***1");

        JsonNode importTaskList = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/admin/directory/users/import/tasks"),
                        "teacher-1", "教师", "org-school", null))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(importTaskList.path("list").isArray()).isTrue();
        assertThat(importTaskList.path("list")).isNotEmpty();

        JsonNode importTaskDetail = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/admin/directory/users/import/tasks/{taskId}", taskId),
                        "teacher-1", "教师", "org-school", null))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(importTaskDetail.path("failedRows").asInt()).isEqualTo(1);
        assertThat(importTaskDetail.path("errors").isArray()).isTrue();

        JsonNode created = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/admin/directory/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "batch-user-1",
                                "password", "Pass@123",
                                "displayName", "批量用户",
                                "role", "STUDENT",
                                "enabled", true
                        ))), "admin-1", "管理员", "org-school", null))
                .andExpect(status().isOk())
                .andReturn());
        String createdUserId = created.path("userId").asText();

        mockMvc.perform(withAuth(post("/api/v1/admin/directory/users/batch/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "userIds", java.util.List.of(createdUserId, "missing-user-id"),
                                "locked", true,
                                "reason", "批量锁定测试"
                        ))), "admin-1", "管理员", "org-school", null))
                .andExpect(status().isOk());

        JsonNode batchStats = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/admin/directory/users/batch/stats"),
                        "teacher-1", "教师", "org-school", null))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(batchStats.path("items").isArray()).isTrue();
        assertThat(batchStats.path("items")).isNotEmpty();
        assertThat(batchStats.path("totalRequested").asInt()).isGreaterThan(0);
        assertThat(batchStats.path("totalFailed").asInt()).isGreaterThanOrEqualTo(1);
    }

    private JsonNode readSuccessData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(root.path("code").asInt()).isEqualTo(200);
        return root.path("data");
    }

    private JsonNode readResponseTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T withAuth(
            T builder,
            String userId,
            String userName,
            String orgUnitId,
            String courseId
    ) {
        T requestBuilder = builder
                .header("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
                .header("x-aigc-token", TEST_ACCESS_TOKEN)
                .header("x-user-id", userId)
                .header("x-user-name", userName)
                .header("x-org-unit-id", orgUnitId);
        if (courseId != null && !courseId.isBlank()) {
            requestBuilder.header("x-course-id", courseId);
        }
        return requestBuilder;
    }
}
