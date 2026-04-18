package com.example.aigc;

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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Test
    void seedTeacherCanLoginAndReadProfile() throws Exception {
        JsonNode login = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "teacher",
                                "password", "Teacher@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        String accessToken = login.path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(login.path("user").path("role").asText()).isEqualTo("TEACHER");

        JsonNode profile = readSuccessData(mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(profile.path("username").asText()).isEqualTo("teacher");
        assertThat(profile.path("role").asText()).isEqualTo("TEACHER");
    }

    @Test
    void loginFailsWhenPasswordIsWrong() throws Exception {
        JsonNode response = readResponseTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "teacher",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(response.path("code").asInt()).isEqualTo(401);
        assertThat(response.path("message").asText()).contains("用户名或密码错误");
    }

    @Test
    void adminCanManageOrgUnitsAndUsers() throws Exception {
        JsonNode login = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "admin",
                                "password", "Admin@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String accessToken = login.path("accessToken").asText();

        JsonNode orgUnit = readSuccessData(mockMvc.perform(post("/api/v1/admin/directory/org-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "数字影视学院",
                                "code", "film-school",
                                "type", "ORGANIZATION"
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode classroom = readSuccessData(mockMvc.perform(post("/api/v1/admin/directory/org-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "2026级1班",
                                "code", "class-2026-1",
                                "type", "CLASSROOM",
                                "parentUnitId", orgUnit.path("unitId").asText()
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode createdUser = readSuccessData(mockMvc.perform(post("/api/v1/admin/directory/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "student.demo",
                                "password", "Student@123",
                                "displayName", "演示学生",
                                "role", "STUDENT",
                                "orgUnitId", orgUnit.path("unitId").asText(),
                                "classroomId", classroom.path("unitId").asText(),
                                "enabled", true
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(createdUser.path("username").asText()).isEqualTo("student.demo");
        assertThat(createdUser.path("classroomId").asText()).isEqualTo(classroom.path("unitId").asText());

        JsonNode users = readSuccessData(mockMvc.perform(get("/api/v1/admin/directory/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(users.path("list").isArray()).isTrue();
        assertThat(users.path("list").toString()).contains("student.demo");

        JsonNode mediaResources = readSuccessData(mockMvc.perform(get("/api/v1/admin/media-resources")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(mediaResources.isArray()).isTrue();
    }

    @Test
    void mediaResourcesCanBeReadByTeacherAndAdminButNotStudent() throws Exception {
        JsonNode teacherLogin = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "teacher",
                                "password", "Teacher@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String teacherToken = teacherLogin.path("accessToken").asText();

        JsonNode teacherMediaResources = readSuccessData(mockMvc.perform(get("/api/v1/admin/media-resources")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("x-aigc-token", teacherToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(teacherMediaResources.isArray()).isTrue();

        JsonNode adminLogin = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "admin",
                                "password", "Admin@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String adminToken = adminLogin.path("accessToken").asText();

        JsonNode adminMediaResources = readSuccessData(mockMvc.perform(get("/api/v1/admin/media-resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("x-aigc-token", adminToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(adminMediaResources.isArray()).isTrue();

        JsonNode studentLogin = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "student",
                                "password", "Student@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String studentToken = studentLogin.path("accessToken").asText();

        JsonNode studentMediaResourcesResponse = readResponseTree(mockMvc.perform(get("/api/v1/admin/media-resources")
                        .header("Authorization", "Bearer " + studentToken)
                        .header("x-aigc-token", studentToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(studentMediaResourcesResponse.path("code").asInt()).isEqualTo(403);
    }

    @Test
    void userListSupportsPaginationAndLockedFilter() throws Exception {
        String unique = "paging-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode login = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "admin",
                                "password", "Admin@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String accessToken = login.path("accessToken").asText();

        JsonNode createdUser = readSuccessData(mockMvc.perform(post("/api/v1/admin/directory/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", unique,
                                "password", "Student@123",
                                "displayName", "分页测试用户",
                                "role", "STUDENT",
                                "enabled", true
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String userId = createdUser.path("userId").asText();

        readSuccessData(mockMvc.perform(put("/api/v1/admin/directory/users/" + userId + "/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "locked", true,
                                "reason", "manual"
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode users = readSuccessData(mockMvc.perform(get("/api/v1/admin/directory/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("x-aigc-token", accessToken)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("keyword", unique)
                        .param("locked", "true"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(users.path("total").asInt()).isEqualTo(1);
        assertThat(users.path("list").size()).isEqualTo(1);
        assertThat(users.path("list").get(0).path("username").asText()).isEqualTo(unique);
        assertThat(users.path("list").get(0).path("locked").asBoolean()).isTrue();
    }

    @Test
    void adminCanUnlockAndForceLogoutUser() throws Exception {
        JsonNode adminLogin = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "admin",
                                "password", "Admin@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String adminToken = adminLogin.path("accessToken").asText();

        readSuccessData(mockMvc.perform(put("/api/v1/admin/directory/users/teacher-001/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("x-aigc-token", adminToken)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "locked", true,
                                "reason", "manual"
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode lockLoginFail = readResponseTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "teacher",
                                "password", "Teacher@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(lockLoginFail.path("code").asInt()).isEqualTo(403);

        readSuccessData(mockMvc.perform(put("/api/v1/admin/directory/users/teacher-001/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("x-aigc-token", adminToken)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "locked", false
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode teacherLogin = readSuccessData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "teacher",
                                "password", "Teacher@123"
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        String teacherToken = teacherLogin.path("accessToken").asText();

        readSuccessData(mockMvc.perform(post("/api/v1/admin/directory/users/teacher-001/force-logout")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("x-aigc-token", adminToken))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode profileFail = readResponseTree(mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + teacherToken)
                        .header("x-aigc-token", teacherToken))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(profileFail.path("code").asInt()).isEqualTo(401);
    }

    private JsonNode readSuccessData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(root.path("code").asInt()).isEqualTo(200);
        return root.path("data");
    }

    private JsonNode readResponseTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
