package com.example.aigc;

import com.example.aigc.service.LocalAssetFileService;
import com.example.aigc.service.ProviderHttpGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ScriptProjectOptimizeIntegrationTest {

    private static final Path DATA_DIR = createDataDir();
    private static final String TEST_ACCESS_TOKEN = "dev-local-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private LocalAssetFileService localAssetFileService;

    @MockitoSpyBean
    private ProviderHttpGateway providerHttpGateway;

    private boolean optimizeReturnInvalidJson = false;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("aigc.storage.data-dir", () -> DATA_DIR.toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-optimize;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("aigc.pipeline.video.max-parallel", () -> 2);
        registry.add("aigc.pipeline.video.poll-interval-ms", () -> 10L);
        registry.add("aigc.pipeline.video.max-retries", () -> 1);
    }

    @BeforeEach
    void setUp() throws IOException {
        optimizeReturnInvalidJson = false;
        clearDirectory(DATA_DIR);

        doAnswer(invocation -> {
            // openai chat format: payload messages[system/user], mock returns choices[0].message.content.
            Map<String, Object> payload = invocation.getArgument(4);
            String systemText = extractSystemText(payload);

            String content;
            if (systemText.contains("`scenes`") && systemText.contains("`segments`")) {
                content = buildOptimizeScenesContent(optimizeReturnInvalidJson);
            } else if (systemText.contains("`characters`")) {
                content = buildOptimizeCharactersContent();
            } else if (systemText.contains("`props`")) {
                content = buildOptimizePropsContent();
            } else {
                // refine call
                content = buildRefineContent();
            }

            Map<String, Object> response = new LinkedHashMap<>();
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("content", content);
            response.put("choices", List.of(Map.of("message", message)));
            return response;
        }).when(providerHttpGateway).invokeChat(any(), anyString(), anyString(), any(), anyMap(), any());
    }

    @AfterAll
    static void tearDown() throws IOException {
        deleteRecursively(DATA_DIR);
    }

    @Test
    void optimizeScenesCharactersProps_mergeById_andRestoreToBeforeCharacter() throws Exception {
        String projectId = createTextProject(
                """
                        第一幕：清晨，古镇街口。
                        阿满背着画箱穿过薄雾，准备在天亮前完成第一张写生。
                        路人乙从远处经过，木制路牌轻轻晃动。
                        """,
                "gpt-4o-mini"
        );

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/refine", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        JsonNode afterScenes = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/optimize/scenes", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        assertThat(afterScenes.path("structuredScript").path("scenes").get(0).path("estimatedDurationSec").asInt())
                .isEqualTo(30);
        assertThat(afterScenes.path("structuredScript").path("segments").get(0).path("estimatedDurationSec").asInt())
                .isEqualTo(5);
        assertThat(afterScenes.path("structuredScript").path("segments").get(0).path("blocking").asText())
                .isNotBlank();

        JsonNode afterCharacters = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/optimize/characters", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());
        assertThat(afterCharacters.path("structuredScript").path("characters").get(0).path("persona").asText())
                .contains("画师");

        JsonNode afterProps = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/optimize/props", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());
        assertThat(afterProps.path("structuredScript").path("props").get(0).path("creativeUse").asText())
                .contains("把画箱当作镜头");

        JsonNode revisions = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/revisions", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());
        assertThat(revisions.isArray()).isTrue();
        assertThat(revisions.size()).isGreaterThanOrEqualTo(3);

        String beforeCharacterRevisionId = findRevisionIdByKind(revisions, "OPTIMIZE_CHARACTER");
        assertThat(beforeCharacterRevisionId).isNotBlank();

        JsonNode restored = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/revisions/{revisionId}/restore",
                        projectId,
                        beforeCharacterRevisionId
                )))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        // scenes/segments keep optimized fields; characters/props are rolled back.
        assertThat(restored.path("structuredScript").path("scenes").get(0).path("estimatedDurationSec").asInt())
                .isEqualTo(30);
        assertThat(restored.path("structuredScript").path("segments").get(0).path("estimatedDurationSec").asInt())
                .isEqualTo(5);
        assertThat(restored.path("structuredScript").path("characters").get(0).hasNonNull("persona"))
                .isFalse();
        assertThat(restored.path("structuredScript").path("props").get(0).hasNonNull("creativeUse"))
                .isFalse();

        JsonNode revisionsAfterRestore = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/revisions", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());
        assertThat(revisionsAfterRestore.size()).isGreaterThan(revisions.size());
    }

    @Test
    void splitShots_shouldAllocateTargetDurationBySegmentEstimatedDuration() throws Exception {
        String projectId = createTextProject(
                """
                        第一幕：清晨，古镇街口。
                        阿满背着画箱穿过薄雾，准备在天亮前完成第一张写生。
                        路人乙从远处经过，木制路牌轻轻晃动。
                        """,
                "gpt-4o-mini"
        );

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/refine", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        // Optimize scenes/segments to inject estimatedDurationSec.
        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/optimize/scenes", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        JsonNode shots = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/shots/split", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        assertThat(shots.isArray()).isTrue();
        assertThat(shots.size()).isEqualTo(2);

        // targetDuration (from createTextProject) = 12
        // segment estimated durations: 5 and 6 => proportional allocation: 5 and 7
        assertThat(shots.get(0).path("targetDurationSec").asInt()).isEqualTo(5);
        assertThat(shots.get(1).path("targetDurationSec").asInt()).isEqualTo(7);
        assertThat(shots.get(0).path("targetDurationSec").asInt() + shots.get(1).path("targetDurationSec").asInt())
                .isEqualTo(12);
    }

    @Test
    void optimizeNonJson_persistsFailureContext_andReturns502() throws Exception {
        String projectId = createTextProject(
                """
                        夜色下，主角站在街角，准备拨通电话。
                        路人乙从远处经过。
                        """,
                "gpt-4o-mini"
        );

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/refine", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        optimizeReturnInvalidJson = true;

        JsonNode failed = readResponseTree(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/optimize/scenes", projectId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());
        assertThat(failed.path("code").asInt()).isEqualTo(502);
        assertThat(failed.path("message").asText()).contains("模型返回内容不是有效 JSON");

        Path projectRoot = localAssetFileService.resolveProjectRoot(projectId);
        Path failureFile = projectRoot.resolve("documents/optimize-raw-response-scenes.txt");
        assertThat(Files.exists(failureFile)).isTrue();
    }

    private JsonNode readSuccessData(MvcResult result) throws Exception {
        JsonNode root = readResponseTree(result);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        return root.path("data");
    }

    private JsonNode readResponseTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T withScriptAuth(T builder) {
        return builder
                .header("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
                .header("x-aigc-token", TEST_ACCESS_TOKEN)
                .header("x-user-id", "integration-test-user");
    }

    private String extractSystemText(Map<String, Object> payload) {
        Object messages = payload.get("messages");
        if (!(messages instanceof List<?> list)) return "";
        for (Object msg : list) {
            if (!(msg instanceof Map<?, ?> m)) continue;
            Object role = m.get("role");
            if (!"system".equals(String.valueOf(role))) continue;
            Object content = m.get("content");
            return content == null ? "" : String.valueOf(content);
        }
        return "";
    }

    private String createTextProject(String sourceText, String explicitTextModel) throws Exception {
        createTextModelConfig(explicitTextModel);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "Optimize Test Project");
        request.put("sourceText", sourceText);
        request.put("visualStyle", "电影感写实");
        request.put("aspectRatio", "16:9");
        request.put("targetDuration", 12);
        request.put("language", "zh-CN");
        request.put("explicitTextModel", explicitTextModel);

        JsonNode createData = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        assertThat(createData.path("project").path("projectId").asText()).isNotBlank();
        return createData.path("project").path("projectId").asText();
    }

    private void createTextModelConfig(String modelName) throws Exception {
        Map<String, Object> connectionRequest = new LinkedHashMap<>();
        connectionRequest.put("name", "Test OpenAI Connection");
        connectionRequest.put("provider", "openai");
        connectionRequest.put("baseUrl", "https://api.openai.com/v1");
        connectionRequest.put("apiKey", "sk-test-key");
        connectionRequest.put("enabled", true);
        JsonNode connection = readSuccessData(mockMvc.perform(post("/api/v1/connections")
                        .header("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
                        .header("x-aigc-token", TEST_ACCESS_TOKEN)
                        .header("x-user-id", "integration-test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(connectionRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());

        String connectionId = connection.path("id").asText();

        Map<String, Object> modelRequest = new LinkedHashMap<>();
        modelRequest.put("name", "Test Text Model");
        modelRequest.put("provider", "openai");
        modelRequest.put("modelName", modelName);
        modelRequest.put("connectionId", connectionId);
        modelRequest.put("enabled", true);
        modelRequest.put("metadata", Map.of("capabilities", List.of("text")));

        readSuccessData(mockMvc.perform(post("/api/v1/models")
                        .header("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
                        .header("x-aigc-token", TEST_ACCESS_TOKEN)
                        .header("x-user-id", "integration-test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(modelRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn());
    }

    private String findRevisionIdByKind(JsonNode revisions, String kind) {
        for (JsonNode r : revisions) {
            if (kind.equals(r.path("kind").asText())) {
                return r.path("revisionId").asText();
            }
        }
        return "";
    }

    private String buildRefineContent() {
        // Must contain required fields for ScriptWorkflowService.refineStructuredScript.
        return """
                {
                  "title":"古镇清晨",
                  "summary":"阿满在古镇清晨展开写生，镜头聚焦人物与环境关系。",
                  "characters":[
                    {"id":"char-1","name":"阿满","description":"年轻画师","tags":["主角","画师"]},
                    {"id":"char-2","name":"路人乙","description":"只是远景陪衬人物，不参与主要动作","tags":["路人"]}
                  ],
                  "backgrounds":[{"id":"bg-1","name":"古镇街口","description":"清晨薄雾","sceneId":"scene-1"}],
                  "props":[{"id":"prop-1","name":"画箱","description":"木制画箱","tags":["道具"]}],
                  "scenes":[{"id":"scene-1","title":"古镇街口","location":"古镇街口","time":"清晨","atmosphere":"平静","summary":"阿满进入古镇街口。"}],
                  "segments":[
                    {"id":"segment-1","title":"清晨入场","scriptText":"阿满背着画箱穿过古镇街口，停在晨雾与茶摊热气之间。","actionSummary":"主角入场并确定写生位置","cameraMovement":"推镜头","characterIds":["char-1"],"backgroundIds":["bg-1"],"propIds":["prop-1"]},
                    {"id":"segment-2","title":"第二次构图","scriptText":"阿满稍作停留，调整画箱角度，准备开始第二次构图。","actionSummary":"主角完成第二构图动作并进入下一段节奏","cameraMovement":"特写推进","characterIds":["char-2"],"backgroundIds":["bg-1"],"propIds":["prop-1"]}
                  ]
                }
                """;
    }

    private String buildOptimizeScenesContent(boolean invalidJson) {
        if (invalidJson) {
            return "not-json";
        }
        return """
                {
                  "scenes":[{"id":"scene-1","shootingNotes":"机位：中景缓慢推近，突出薄雾中的主角","blocking":"阿满从画面左侧进入，在茶摊前停步","estimatedDurationSec":30}],
                  "segments":[
                    {"id":"segment-1","shootingNotes":"以主角手部与画箱为节奏锚点，呼吸与停顿更明显","blocking":"画箱靠近胸前，视线从路牌扫回取景点","estimatedDurationSec":5},
                    {"id":"segment-2","shootingNotes":"节奏稍加快，突出构图切换后的情绪落点","blocking":"阿满调整画箱后，视线定格在取景点，完成第二落点","estimatedDurationSec":6}
                  ]
                }
                """;
    }

    private String buildOptimizeCharactersContent() {
        return """
                {
                  "characters":[
                    {
                      "id":"char-1",
                      "persona":"画师身份明确：专注取景、对光线变化极敏感，画面里情绪克制但有推进感",
                      "traits":["细致","安静","行动前先观察"],
                      "quirks":"习惯用指尖轻敲画箱边缘来确认画幅尺寸",
                      "relationships":[{"targetId":"char-2","relation":"与路人乙擦肩而过，眼神礼貌但不回应对话"}]
                    },
                    {
                      "id":"char-2",
                      "persona":"路人乙只做环境陪衬：经过时略微停住看一眼再走",
                      "traits":["随和","不抢戏"],
                      "quirks":"路牌晃动时会下意识抬头确认方向",
                      "relationships":[{"targetId":"char-1","relation":"路人路过并带来轻微干扰，强化主角的专注对比"}]
                    }
                  ]
                }
                """;
    }

    private String buildOptimizePropsContent() {
        return """
                {
                  "props":[
                    {
                      "id":"prop-1",
                      "creativeUse":"把画箱当作镜头的节奏道具：主角调整画箱角度时，画面完成一次“从环境到主体”的切换",
                      "sceneRefs":["scene-1"],
                      "importance":"关键道具，用于承接入场后的取景动作与情绪落点",
                      "tags":["道具","写生"]
                    }
                  ]
                }
                """;
    }

    private static Path createDataDir() {
        try {
            return Files.createTempDirectory("aigc-server-optimize-it-");
        } catch (IOException ex) {
            throw new IllegalStateException("创建测试目录失败", ex);
        }
    }

    private static void clearDirectory(Path path) throws IOException {
        deleteRecursively(path);
        Files.createDirectories(path);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted((left, right) -> right.compareTo(left))
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (IOException ex) {
                            throw new IllegalStateException("删除测试目录失败: " + current, ex);
                        }
                    });
        }
    }
}
