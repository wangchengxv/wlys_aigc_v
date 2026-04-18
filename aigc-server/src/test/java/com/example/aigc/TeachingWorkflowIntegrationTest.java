package com.example.aigc;

import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.enums.AssignmentStatus;
import com.example.aigc.repository.TeachingAssignmentRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TeachingWorkflowIntegrationTest {

    private static final String TEST_ACCESS_TOKEN = "dev-local-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeachingAssignmentRepository teachingAssignmentRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-teaching;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Test
    void teacherCanPublishAssignmentAndReviewStudentSubmission() throws Exception {
        JsonNode course = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "AIGC 影视创作实训",
                                "code", "AIGC-2026-S01",
                                "description", "面向影视创作方向的课程"
                        ))), "teacher-1", "张老师", "film-school", null))
                .andExpect(status().isOk())
                .andReturn());
        String courseId = course.path("courseId").asText();

        JsonNode assignment = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/assignments", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "古镇清晨短片作业",
                                "brief", "请围绕古镇清晨创作 15 秒分镜短片",
                                "aspectRatio", "16:9",
                                "targetDuration", 15,
                                "language", "zh-CN",
                                "dueAt", OffsetDateTime.now(ZoneOffset.ofHours(8)).plusDays(7).withNano(0).toString()
                        ))), "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        String assignmentId = assignment.path("assignmentId").asText();

        String projectId = createStudentProject(courseId);

        JsonNode submission = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "projectId", projectId,
                                "note", "已完成第一版提交，请老师点评"
                        ))), "student-1", "李同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        String submissionId = submission.path("submissionId").asText();
        assertThat(submission.path("status").asText()).isEqualTo("SUBMITTED");

        JsonNode teacherSubmissions = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/assignments/{assignmentId}/submissions", assignmentId),
                        "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(teacherSubmissions.isArray()).isTrue();
        assertThat(teacherSubmissions).hasSize(1);
        assertThat(teacherSubmissions.get(0).path("studentUserId").asText()).isEqualTo("student-1");

        JsonNode reviewed = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/submissions/{submissionId}/review", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "status", "REVIEWED",
                                "score", 92,
                                "comment", "节奏和氛围很好，下一版可以强化镜头过渡。"
                        ))), "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(reviewed.path("status").asText()).isEqualTo("REVIEWED");
        assertThat(reviewed.path("score").asInt()).isEqualTo(92);

        JsonNode reviewRecords = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/submissions/{submissionId}/reviews", submissionId),
                        "student-1", "李同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(reviewRecords).hasSize(1);
        assertThat(reviewRecords.get(0).path("reviewerUserId").asText()).isEqualTo("teacher-1");
        assertThat(reviewRecords.get(0).path("status").asText()).isEqualTo("REVIEWED");

        JsonNode studentSubmissions = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/assignments/{assignmentId}/submissions", assignmentId),
                        "student-1", "李同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(studentSubmissions).hasSize(1);
        assertThat(studentSubmissions.get(0).path("submissionId").asText()).isEqualTo(submissionId);

        JsonNode teacherProjectDetail = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}", projectId),
                        "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(teacherProjectDetail.path("project").path("projectId").asText()).isEqualTo(projectId);

        JsonNode blockedDelete = readResponseTree(mockMvc.perform(withAuth(delete("/api/v1/script-projects/{projectId}", projectId),
                        "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(blockedDelete.path("code").asInt()).isEqualTo(403);
        assertThat(blockedDelete.path("message").asText()).contains("修改");

        JsonNode closedAssignment = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/assignments/{assignmentId}/status", courseId, assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "CLOSED"))),
                "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(closedAssignment.path("status").asText()).isEqualTo("CLOSED");

        JsonNode blockedSubmission = readResponseTree(mockMvc.perform(withAuth(post("/api/v1/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "projectId", projectId,
                                "note", "关闭后再次提交"
                        ))), "student-1", "李同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(blockedSubmission.path("code").asInt()).isEqualTo(400);
        assertThat(blockedSubmission.path("message").asText()).contains("作业已关闭");

        JsonNode archivedCourse = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/archive", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("archived", true))),
                "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(archivedCourse.path("archived").asBoolean()).isTrue();

        JsonNode courseAuditLogs = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/audit-logs")
                        .param("entityType", "COURSE")
                        .param("entityId", courseId),
                "teacher-1", "张老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(courseAuditLogs.isArray()).isTrue();
        assertThat(courseAuditLogs).isNotEmpty();
        assertThat(courseAuditLogs.get(0).path("action").asText()).isEqualTo("COURSE_ARCHIVED");
    }

    @Test
    void courseScopedTemplateCanFlowThroughAssignmentAndSubmission() throws Exception {
        JsonNode course = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "课程模板实训",
                                "code", "AIGC-TEMPLATE-01",
                                "description", "验证课程模板在作业链路中的使用"
                        ))), "teacher-2", "王老师", "film-school", null))
                .andExpect(status().isOk())
                .andReturn());
        String courseId = course.path("courseId").asText();

        JsonNode styleTemplate = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/style-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "scope", "COURSE",
                                "name", "古镇晨雾模板",
                                "category", "课程指定",
                                "traits", "清晨薄雾、纪实电影感、暖冷对比",
                                "fullPrompt", "古镇清晨，薄雾、石板路、暖冷对比、纪实电影感",
                                "courseId", courseId
                        ))), "teacher-2", "王老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        String styleTemplateId = styleTemplate.path("templateId").asText();

        JsonNode assignment = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/assignments", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "课程模板绑定作业",
                                "brief", "必须使用课程提供的模板进行创作",
                                "styleTemplateId", styleTemplateId,
                                "aspectRatio", "16:9",
                                "targetDuration", 20,
                                "language", "zh-CN",
                                "dueAt", OffsetDateTime.now(ZoneOffset.ofHours(8)).plusDays(5).withNano(0).toString()
                        ))), "teacher-2", "王老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        String assignmentId = assignment.path("assignmentId").asText();
        assertThat(assignment.path("styleTemplateId").asText()).isEqualTo(styleTemplateId);

        String matchedProjectId = createStudentProject(courseId, styleTemplateId, "绑定模板的学生项目", "student-2", "周同学");
        JsonNode successSubmission = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "projectId", matchedProjectId,
                                "note", "已按课程模板完成创作"
                        ))), "student-2", "周同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(successSubmission.path("status").asText()).isEqualTo("SUBMITTED");

        String unmatchedProjectId = createStudentProject(courseId, null, "未绑定模板的学生项目", "student-2", "周同学");
        JsonNode blockedSubmission = readResponseTree(mockMvc.perform(withAuth(post("/api/v1/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "projectId", unmatchedProjectId,
                                "note", "未绑定模板"
                        ))), "student-2", "周同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(blockedSubmission.path("code").asInt()).isEqualTo(400);
        assertThat(blockedSubmission.path("message").asText()).contains("风格模板");
    }

    @Test
    void assignmentCreateRequiresDueAt() throws Exception {
        JsonNode course = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "作业截止时间校验课程",
                                "code", "AIGC-DUEAT-VALIDATION-01",
                                "description", "验证作业最晚提交时间必填"
                        ))), "teacher-3", "赵老师", "film-school", null))
                .andExpect(status().isOk())
                .andReturn());
        String courseId = course.path("courseId").asText();

        JsonNode badRequest = readResponseTree(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/assignments", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "缺失最晚提交时间的作业",
                                "brief", "用于校验 dueAt 必填",
                                "aspectRatio", "16:9",
                                "targetDuration", 15,
                                "language", "zh-CN"
                        ))), "teacher-3", "赵老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(badRequest.path("code").asInt()).isEqualTo(400);
        assertThat(badRequest.path("message").asText()).isEqualTo("最晚提交时间不能为空");
    }

    @Test
    void assignmentCreateRequiresFutureDueAt() throws Exception {
        JsonNode course = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "作业截止时间未来校验课程",
                                "code", "AIGC-DUEAT-VALIDATION-02",
                                "description", "验证作业最晚提交时间必须晚于当前时间"
                        ))), "teacher-4", "刘老师", "film-school", null))
                .andExpect(status().isOk())
                .andReturn());
        String courseId = course.path("courseId").asText();

        JsonNode badRequest = readResponseTree(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/assignments", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "过去时间作业",
                                "brief", "用于校验 dueAt 时间合法性",
                                "aspectRatio", "16:9",
                                "targetDuration", 15,
                                "language", "zh-CN",
                                "dueAt", OffsetDateTime.now(ZoneOffset.ofHours(8)).minusMinutes(1).withNano(0).toString()
                        ))), "teacher-4", "刘老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(badRequest.path("code").asInt()).isEqualTo(400);
        assertThat(badRequest.path("message").asText()).isEqualTo("最晚提交时间必须晚于当前时间");
    }

    @Test
    void dueAtCanBlockSubmissionWhileLegacyAssignmentWithoutDueAtStillWorks() throws Exception {
        JsonNode course = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "作业截止时间提交流程校验课程",
                                "code", "AIGC-DUEAT-SUBMISSION-01",
                                "description", "验证截止后禁止提交，历史不限时作业继续可提交"
                        ))), "teacher-5", "周老师", "film-school", null))
                .andExpect(status().isOk())
                .andReturn());
        String courseId = course.path("courseId").asText();

        JsonNode assignment = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/courses/{courseId}/assignments", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "截止拦截测试作业",
                                "brief", "先创建后手动调整为过期",
                                "aspectRatio", "16:9",
                                "targetDuration", 15,
                                "language", "zh-CN",
                                "dueAt", OffsetDateTime.now(ZoneOffset.ofHours(8)).plusDays(3).withNano(0).toString()
                        ))), "teacher-5", "周老师", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        String expiredAssignmentId = assignment.path("assignmentId").asText();
        TeachingAssignment expiredAssignment = teachingAssignmentRepository.findById(expiredAssignmentId)
                .orElseThrow(() -> new IllegalStateException("测试作业不存在"));
        expiredAssignment.dueAt = Instant.now().minusSeconds(30);
        expiredAssignment.updatedAt = Instant.now();
        teachingAssignmentRepository.save(expiredAssignment);

        String blockedProjectId = createStudentProject(courseId, null, "截止后提交测试项目", "student-5", "杨同学");
        JsonNode blockedSubmission = readResponseTree(mockMvc.perform(withAuth(post("/api/v1/assignments/{assignmentId}/submissions", expiredAssignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "projectId", blockedProjectId,
                                "note", "截止后提交应被拒绝"
                        ))), "student-5", "杨同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(blockedSubmission.path("code").asInt()).isEqualTo(400);
        assertThat(blockedSubmission.path("message").asText()).contains("作业已截止");

        TeachingAssignment legacyAssignment = new TeachingAssignment();
        legacyAssignment.assignmentId = "assignment-legacy-" + UUID.randomUUID().toString().substring(0, 8);
        legacyAssignment.courseId = courseId;
        legacyAssignment.title = "历史不限时作业";
        legacyAssignment.brief = "模拟历史数据：未设置 dueAt";
        legacyAssignment.status = AssignmentStatus.PUBLISHED;
        legacyAssignment.ownerId = "teacher-5";
        legacyAssignment.ownerName = "周老师";
        legacyAssignment.createdAt = Instant.now();
        legacyAssignment.updatedAt = legacyAssignment.createdAt;
        legacyAssignment.dueAt = null;
        teachingAssignmentRepository.save(legacyAssignment);

        String legacyProjectId = createStudentProject(courseId, null, "历史作业提交流程项目", "student-5", "杨同学");
        JsonNode legacySubmission = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/assignments/{assignmentId}/submissions", legacyAssignment.assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "projectId", legacyProjectId,
                                "note", "历史不限时作业提交应成功"
                        ))), "student-5", "杨同学", "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(legacySubmission.path("status").asText()).isEqualTo("SUBMITTED");
    }

    private String createStudentProject(String courseId) throws Exception {
        return createStudentProject(courseId, null, "学生短片项目", "student-1", "李同学");
    }

    private String createStudentProject(
            String courseId,
            String styleTemplateId,
            String projectName,
            String studentUserId,
            String studentUserName
    ) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", projectName);
        request.put("sourceText", "清晨的古镇街口，学生背着画箱走入晨雾。");
        request.put("visualStyle", "电影感");
        request.put("aspectRatio", "16:9");
        request.put("targetDuration", 15);
        request.put("language", "zh-CN");
        request.put("courseId", courseId);
        if (styleTemplateId != null && !styleTemplateId.isBlank()) {
            request.put("styleTemplateId", styleTemplateId);
        }

        JsonNode project = readSuccessData(mockMvc.perform(withAuth(post("/api/v1/script-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)), studentUserId, studentUserName, "film-school", courseId))
                .andExpect(status().isOk())
                .andReturn());
        return project.path("project").path("projectId").asText();
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
