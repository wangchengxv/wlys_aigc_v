package com.example.aigc;

import com.example.aigc.entity.ExportPackageTask;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.enums.ContentReviewStatus;
import com.example.aigc.enums.ExportPackageTaskStatus;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.enums.UserRole;
import com.example.aigc.service.AuditLogService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.ScriptProjectService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class OperationsDashboardIntegrationTest {

    private static final String TEST_ACCESS_TOKEN = "dev-local-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptProjectService scriptProjectService;

    @Autowired
    private AuditLogService auditLogService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-operations-dashboard;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Test
    void teacherAndAdminCanViewDashboardWhileStudentIsDenied() throws Exception {
        String courseId = createCourse("teacher-1", "张老师");
        String assignmentId = createAssignment(courseId, "teacher-1", "张老师");

        String draftProjectId = createProject(courseId, "草稿项目", "student-1", "李同学");
        String generatingProjectId = createProject(courseId, "生成中项目", "student-2", "周同学");
        String finalReadyProjectId = createProject(courseId, "成片完成项目", "student-3", "吴同学");
        String pendingReviewProjectId = createProject(courseId, "待审核项目", "student-4", "郑同学");
        String approvedProjectId = createProject(courseId, "审核通过项目", "student-5", "王同学");
        String rejectedProjectId = createProject(courseId, "审核驳回项目", "student-6", "孙同学");

        createSubmission(assignmentId, pendingReviewProjectId, "student-4", "郑同学", courseId);
        createSubmission(assignmentId, approvedProjectId, "student-5", "王同学", courseId);

        configureProject(draftProjectId, ProjectStatus.DRAFT, ContentReviewStatus.NOT_SUBMITTED, null);
        configureProject(generatingProjectId, ProjectStatus.VIDEO_GENERATING, ContentReviewStatus.NOT_SUBMITTED, null);
        configureProject(finalReadyProjectId, ProjectStatus.FINAL_COMPOSITION_READY, ContentReviewStatus.NOT_SUBMITTED, Instant.now().minusSeconds(600));
        configureProject(pendingReviewProjectId, ProjectStatus.FINAL_COMPOSITION_READY, ContentReviewStatus.PENDING, Instant.now().minusSeconds(480));
        configureProject(approvedProjectId, ProjectStatus.EXPORT_PACKAGE_READY, ContentReviewStatus.APPROVED, Instant.now().minusSeconds(360));
        configureProject(rejectedProjectId, ProjectStatus.EXPORT_PACKAGE_READY, ContentReviewStatus.REJECTED, Instant.now().minusSeconds(240));

        recordProjectAudit("PROJECT_REVIEW_SUBMITTED", pendingReviewProjectId, "student-4", "郑同学", UserRole.STUDENT, courseId);
        recordProjectAudit("PROJECT_REVIEW_APPROVED", approvedProjectId, "teacher-1", "张老师", UserRole.TEACHER, courseId);
        recordProjectAudit("PROJECT_REVIEW_REJECTED", rejectedProjectId, "teacher-1", "张老师", UserRole.TEACHER, courseId);

        JsonNode teacherDashboard = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/operations/dashboard"),
                        "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());

        Map<String, Long> metrics = toLongMap(teacherDashboard.path("overviewCards"), "value");
        assertThat(metrics).containsEntry("courseCount", 1L);
        assertThat(metrics).containsEntry("assignmentCount", 1L);
        assertThat(metrics).containsEntry("submissionCount", 2L);
        assertThat(metrics).containsEntry("projectCount", 6L);
        assertThat(metrics).containsEntry("exportPackageCount", 4L);
        assertThat(metrics).containsEntry("pendingReviewCount", 1L);

        Map<String, Long> distribution = toLongMap(teacherDashboard.path("statusDistribution"), "count");
        assertThat(distribution).containsEntry("draft", 1L);
        assertThat(distribution).containsEntry("generating", 1L);
        assertThat(distribution).containsEntry("finalReady", 1L);
        assertThat(distribution).containsEntry("pendingReview", 1L);
        assertThat(distribution).containsEntry("approved", 1L);
        assertThat(distribution).containsEntry("rejected", 1L);

        JsonNode recentActivities = teacherDashboard.path("recentActivities");
        assertThat(recentActivities.isArray()).isTrue();
        assertThat(recentActivities).isNotEmpty();
        assertThat(containsActivity(recentActivities, "PROJECT_REVIEW_SUBMITTED", "项目提审")).isTrue();
        assertThat(containsActivity(recentActivities, "PROJECT_REVIEW_APPROVED", "审核通过")).isTrue();
        assertThat(containsActivity(recentActivities, "PROJECT_REVIEW_REJECTED", "审核驳回")).isTrue();
        assertThat(containsActivity(recentActivities, "EXPORT_PACKAGE_COMPLETED", "导出包完成")).isTrue();
        assertThat(allActivitiesExposeEntityHints(recentActivities)).isTrue();

        JsonNode adminDashboard = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/operations/dashboard"),
                        "admin-1", "管理员", "film-school", null))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(adminDashboard.path("overviewCards").size()).isEqualTo(6);

        JsonNode studentResponse = readResponseTree(mockMvc.perform(withAuth(get("/api/v1/operations/dashboard"),
                        "student-1", "李同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(studentResponse.path("code").asInt()).isEqualTo(403);
        assertThat(studentResponse.path("message").asText()).contains("统计看板");
    }

    private void configureProject(
            String projectId,
            ProjectStatus status,
            ContentReviewStatus reviewStatus,
            Instant exportFinishedAt
    ) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        aggregate.project.status = status;
        aggregate.project.contentReviewStatus = reviewStatus;
        aggregate.exportPackageTasks.clear();
        if (exportFinishedAt != null) {
            ExportPackageTask task = new ExportPackageTask();
            task.exportPackageTaskId = "export-" + projectId;
            task.projectId = projectId;
            task.status = ExportPackageTaskStatus.SUCCESS;
            task.finishedAt = exportFinishedAt;
            aggregate.exportPackageTasks.add(task);
        }
        scriptProjectService.save(aggregate);
    }

    private void recordProjectAudit(
            String action,
            String projectId,
            String userId,
            String userName,
            UserRole role,
            String courseId
    ) {
        auditLogService.record(
                new RequestUserContext(userId, userName, role, "film-school", courseId, true),
                action,
                "SCRIPT_PROJECT",
                projectId,
                Map.of("source", "operations-dashboard-test")
        );
    }

    private String createCourse(String teacherUserId, String teacherUserName) throws Exception {
        JsonNode course = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "运营统计课程",
                                "code", "OPS-DASHBOARD-01",
                                "description", "用于验证统计看板聚合逻辑"
                        ))), teacherUserId, teacherUserName, "film-school", null))
                .andExpect(status().isOk())
                .andReturn());
        return course.path("courseId").asText();
    }

    private String createAssignment(String courseId, String teacherUserId, String teacherUserName) throws Exception {
        JsonNode assignment = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/assignments", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "统计看板样例作业",
                                "brief", "用于生成提交数样例",
                                "aspectRatio", "16:9",
                                "targetDuration", 15,
                                "language", "zh-CN",
                                "dueAt", OffsetDateTime.now(ZoneOffset.ofHours(8)).plusDays(3).withNano(0).toString()
                        ))), teacherUserId, teacherUserName, "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        return assignment.path("assignmentId").asText();
    }

    private void createSubmission(
            String assignmentId,
            String projectId,
            String studentUserId,
            String studentUserName,
            String courseId
    ) throws Exception {
        mockMvc.perform(withAuth(post("/api/v1/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "projectId", projectId,
                                "note", "用于统计看板测试的提交记录"
                        ))), studentUserId, studentUserName, "film-school", courseId))
                .andExpect(status().isOk());
    }

    private String createProject(
            String courseId,
            String projectName,
            String studentUserId,
            String studentUserName
    ) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", projectName);
        request.put("sourceText", "清晨古镇里，学生准备完成短片创作。");
        request.put("visualStyle", "电影感");
        request.put("aspectRatio", "16:9");
        request.put("targetDuration", 15);
        request.put("language", "zh-CN");
        request.put("courseId", courseId);

        JsonNode project = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/script-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)),
                studentUserId, studentUserName, "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        return project.path("project").path("projectId").asText();
    }

    private Map<String, Long> toLongMap(JsonNode items, String valueField) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (JsonNode item : items) {
            values.put(item.path("key").asText(), item.path(valueField).asLong());
        }
        return values;
    }

    private boolean containsActivity(JsonNode items, String action, String label) {
        for (JsonNode item : items) {
            if (action.equals(item.path("action").asText()) && label.equals(item.path("label").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean allActivitiesExposeEntityHints(JsonNode items) {
        for (JsonNode item : items) {
            if (item.path("summary").asText().isBlank()) {
                return false;
            }
            if (item.path("entityType").asText().isBlank()) {
                return false;
            }
        }
        return true;
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
