package com.example.aigc;

import com.example.aigc.entity.ExportPackageTask;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.enums.ExportPackageTaskStatus;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.model.WorkflowModelKey;
import com.example.aigc.service.ScriptProjectService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ScriptProjectWorkflowIntegrationTest {

    private static final Path DATA_DIR = createDataDir();
    /** 与默认 {@code aigc.auth.access-token} 及前端联调一致 */
    private static final String TEST_ACCESS_TOKEN = "dev-local-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptProjectService scriptProjectService;

    @MockitoSpyBean
    private LocalAssetFileService localAssetFileService;

    @MockitoSpyBean
    private ProviderHttpGateway providerHttpGateway;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("aigc.storage.data-dir", () -> DATA_DIR.toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-workflow;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("aigc.ark.api-key", () -> "test-ark-api-key");
        registry.add("aigc.pipeline.video.max-parallel", () -> 2);
        registry.add("aigc.pipeline.video.poll-interval-ms", () -> 10L);
        registry.add("aigc.pipeline.video.max-retries", () -> 2);
    }

    @BeforeEach
    void setUp() throws IOException {
        clearDirectory(DATA_DIR);
        doAnswer(invocation -> {
            String projectId = invocation.getArgument(0);
            String relativePath = invocation.getArgument(1);
            String mediaType = invocation.getArgument(2);
            String remoteUrl = invocation.getArgument(3);
            byte[] payload = ("stub-remote:" + mediaType + ":" + relativePath + ":" + remoteUrl)
                    .getBytes(StandardCharsets.UTF_8);
            return localAssetFileService.storeBytes(projectId, relativePath, mediaType, payload);
        }).when(localAssetFileService).storeRemote(anyString(), anyString(), anyString(), anyString());
        doAnswer(invocation -> {
            Map<String, Object> response = new LinkedHashMap<>();
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("content", """
                    {
                      "title":"古镇清晨",
                      "summary":"阿满在古镇清晨展开写生，镜头聚焦人物与环境关系。",
                      "characters":[{"id":"char-1","name":"阿满","description":"年轻画师"}],
                      "backgrounds":[{"id":"bg-1","name":"古镇街口","description":"清晨薄雾"}],
                      "props":[{"id":"prop-1","name":"画箱","description":"木制画箱"}],
                      "scenes":[{"id":"scene-1","title":"古镇街口","location":"古镇街口","time":"清晨","atmosphere":"平静"}],
                      "segments":[{"id":"segment-1","title":"清晨入场","scriptText":"阿满背着画箱穿过古镇街口。","actionSummary":"主角入场","cameraMovement":"推镜头","characterIds":["char-1"],"backgroundIds":["bg-1"],"propIds":["prop-1"]}]
                    }
                    """);
            response.put("choices", List.of(Map.of("message", message)));
            return response;
        }).when(providerHttpGateway).invokeChat(any(), anyString(), anyString(), any(), anyMap(), any());
        doAnswer(invocation -> Map.of(
                "data", List.of(Map.of("url", "https://example.test/generated-image.png"))
        )).when(providerHttpGateway).generateImage(any(), anyString(), anyString(), anyMap(), any());
    }

    @AfterAll
    static void tearDown() throws IOException {
        deleteRecursively(DATA_DIR);
    }

    @Test
    void endToEndWorkflowCompletesAndVideoOnlyRequiresReferencedAssets() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，准备在天亮前完成第一张写生。
                一个路人从远处经过，木制路牌在街角轻轻晃动。
                """, "gpt-4o-mini");

        JsonNode refined = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/refine", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refined.path("projectId").asText()).isEqualTo(projectId);
        assertThat(refined.path("refinedMarkdown").asText()).contains("## 故事摘要");
        assertThat(refined.path("structuredScript").path("segments").isArray()).isTrue();
        assertThat(refined.path("structuredScript").path("segments").size()).isGreaterThan(0);

        JsonNode refinedWithBrief = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects/{projectId}/refine-with-brief", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of("briefPrompt", "让节奏更紧凑，并突出阿满在清晨前的紧张与期待。")))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refinedWithBrief.path("projectId").asText()).isEqualTo(projectId);
        assertThat(refinedWithBrief.path("refinedMarkdown").asText()).contains("## 故事摘要");
        assertThat(refinedWithBrief.path("structuredScript").path("segments").isArray()).isTrue();
        assertThat(refinedWithBrief.path("structuredScript").path("segments").size()).isGreaterThan(0);

        Map<String, Object> manualStructuredScript = buildManualStructuredScript();
        Map<String, Object> updateScriptRequest = new LinkedHashMap<>();
        updateScriptRequest.put("refinedMarkdown", """
                # 古镇清晨

                - 阿满背着画箱穿过古镇街口
                - 路人乙只作为环境陪衬出现，不参与镜头主动作
                """);
        updateScriptRequest.put("structuredScript", manualStructuredScript);

        JsonNode updatedScript = readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/script", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateScriptRequest))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(updatedScript.path("structuredScript").path("characters").size()).isEqualTo(2);
        assertThat(updatedScript.path("structuredScript").path("props").size()).isEqualTo(2);

        JsonNode extractedCharacters = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/characters", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedBackgrounds = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/backgrounds", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedProps = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/props", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(extractedCharacters.size()).isEqualTo(2);
        assertThat(extractedBackgrounds.size()).isEqualTo(1);
        assertThat(extractedProps.size()).isEqualTo(2);

        JsonNode assets = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/assets", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(assets.size()).isEqualTo(5);

        String firstCharacterAssetId = extractedCharacters.get(0).path("assetId").asText();
        Map<String, Object> updateAssetRequest = new LinkedHashMap<>();
        updateAssetRequest.put("name", "阿满（定稿）");
        updateAssetRequest.put("description", "主角视觉设定定稿，保留画箱与清晨薄雾氛围。");
        updateAssetRequest.put("tags", List.of("角色", "主视觉", "定稿"));
        updateAssetRequest.put("promptDraft", "电影感写实，阿满背着画箱走过古镇清晨街口。");
        JsonNode updatedAsset = readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/assets/{assetId}", projectId, firstCharacterAssetId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateAssetRequest))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(updatedAsset.path("name").asText()).isEqualTo("阿满（定稿）");
        assertThat(updatedAsset.path("tags").size()).isEqualTo(3);

        JsonNode shots = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/shots/split", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(shots.size()).isEqualTo(1);

        Set<String> allAssetIds = jsonTextSet(assets, "assetId");
        Set<String> requiredAssetIds = collectShotAssetIds(shots);
        assertThat(requiredAssetIds).contains(firstCharacterAssetId);
        assertThat(allAssetIds).hasSize(5);
        assertThat(requiredAssetIds).hasSize(3);
        assertThat(allAssetIds).isNotEqualTo(requiredAssetIds);

        String selectedImageFileId = null;
        boolean regeneratedOnce = false;
        for (JsonNode asset : assets) {
            String assetId = asset.path("assetId").asText();
            if (!requiredAssetIds.contains(assetId)) {
                continue;
            }

            JsonNode generatedKeyframes = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate", projectId, assetId)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(generatedKeyframes.size()).isEqualTo(2);

            String confirmedKeyframeId = generatedKeyframes.get(0).path("keyframeId").asText();
            JsonNode confirmedKeyframe = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/keyframes/{keyframeId}/confirm", projectId, confirmedKeyframeId)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(confirmedKeyframe.path("selected").asBoolean()).isTrue();
            if (selectedImageFileId == null) {
                selectedImageFileId = confirmedKeyframe.path("imageFileId").asText();
            }

            if (!regeneratedOnce) {
                JsonNode regenerated = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/keyframes/{keyframeId}/regenerate", projectId, confirmedKeyframeId)))
                        .andExpect(status().isOk())
                        .andReturn());
                assertThat(regenerated.size()).isEqualTo(2);
                regeneratedOnce = true;
            }
        }

        JsonNode keyframes = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/keyframes", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(keyframes.size()).isEqualTo(8);
        assertThat(countSelected(keyframes)).isEqualTo(requiredAssetIds.size());
        assertThat(selectedAssetIds(keyframes)).isEqualTo(requiredAssetIds);
        assertThat(selectedImageFileId).isNotBlank();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/video/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode pipeline = waitForVideoCompletion(projectId);
        assertThat(pipeline.path("projectStatus").asText()).isEqualTo("VIDEO_READY");
        assertThat(pipeline.path("latestRun").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(pipeline.path("failedCount").asInt()).isZero();
        assertThat(pipeline.path("successCount").asInt()).isEqualTo(shots.size());

        JsonNode videoTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/video/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(videoTasks.size()).isEqualTo(1);
        assertThat(videoTasks.get(0).path("status").asText()).isEqualTo("SUCCESS");
        String videoFileId = videoTasks.get(0).path("resultVideoFileId").asText();
        assertThat(videoFileId).isNotBlank();

        MvcResult previewImage = mockMvc.perform(get("/api/v1/files/{fileId}", selectedImageFileId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(previewImage.getResponse().getContentType()).startsWith("image/png");
        assertThat(previewImage.getResponse().getContentAsByteArray()).isNotEmpty();

        MvcResult downloadVideo = mockMvc.perform(get("/api/v1/files/{fileId}/download", videoFileId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(downloadVideo.getResponse().getContentType()).startsWith("video/mp4");
        assertThat(downloadVideo.getResponse().getHeader("Content-Disposition")).contains("attachment");
        assertThat(downloadVideo.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void revisionsWithNullElementsStillReturnsEmptyList() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，准备在天亮前完成第一张写生。
                """, "gpt-4o-mini");

        JsonNode revisions = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/revisions", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(revisions.isArray()).isTrue();
    }

    @Test
    void refineFailsWhenExplicitTextModelIsNotConfigured() throws Exception {
        String projectId = createTextProject("""
                夜色下，主角站在街角，准备拨通电话。
                """, "missing-text-model");

        JsonNode failedRefine = readResponseTree(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/refine", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(failedRefine.path("code").asInt()).isEqualTo(400);
        assertThat(failedRefine.path("message").asText()).contains("未命中已配置的文本模型");

        JsonNode detail = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(detail.path("project").path("status").asText()).isEqualTo("FAILED");

        JsonNode pipeline = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(pipeline.path("projectStatus").asText()).isEqualTo("FAILED");
    }

    @Test
    void docxUploadRoundTripAndDeleteCleansProjectFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script-smoke.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                createDocx(
                        "第一幕：清晨，古镇街口。",
                        "阿满背着画箱穿过薄雾，看见远处的茶摊升起热气。",
                        "她停下脚步，决定在天亮前完成第一张写生。"
                )
        );

        JsonNode uploadData = readSuccessData(mockMvc.perform(
                        withScriptAuth(multipart("/api/v1/script-projects/upload")
                                .file(file)
                                .param("name", "DOCX Smoke Project")
                                .param("language", "zh-CN")))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode project = uploadData.path("project");
        String projectId = project.path("projectId").asText();
        assertThat(projectId).isNotBlank();
        assertThat(project.path("sourceType").asText()).isEqualTo("upload");
        assertThat(project.path("uploadedSourceFileId").asText()).isNotBlank();
        assertThat(uploadData.path("documents").size()).isEqualTo(2);

        JsonNode script = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/script", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(script.path("originalText").asText())
                .contains("第一幕：清晨，古镇街口。")
                .contains("阿满背着画箱穿过薄雾")
                .contains("她停下脚步，决定在天亮前完成第一张写生。");

        Path projectRoot = localAssetFileService.resolveProjectRoot(projectId);
        assertThat(Files.exists(projectRoot)).isTrue();
        assertThat(Files.exists(projectRoot.resolve("documents/original-script.txt"))).isTrue();
        assertThat(Files.exists(projectRoot.resolve("documents/uploads/script-smoke.docx"))).isTrue();

        mockMvc.perform(withScriptAuth(delete("/api/v1/script-projects/{projectId}", projectId)))
                .andExpect(status().isOk());

        JsonNode detailAfterDelete = readResponseTree(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(detailAfterDelete.path("code").asInt()).isEqualTo(404);
        assertThat(detailAfterDelete.path("message").asText()).isEqualTo("剧本工程不存在或已删除");

        // 数据从数据库删除后，接口读取应返回 404（已在上面断言）。
    }

    @Test
    void createScriptProjectAcceptsFreeFormPresetAndLongVisualStyle() throws Exception {
        Map<String, Object> freeForm = new LinkedHashMap<>();
        freeForm.put("name", "自由风格项目");
        freeForm.put("sourceText", "第一幕：测试自由描述。");
        freeForm.put("visualStyle", "完全自定义的视觉描述，不必匹配固定 key。");
        freeForm.put("aspectRatio", "16:9");
        freeForm.put("targetDuration", 15);
        freeForm.put("language", "中文");
        JsonNode createdFree = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(freeForm))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(createdFree.path("project").path("visualStyle").asText()).contains("完全自定义");

        Map<String, Object> preset = new LinkedHashMap<>();
        preset.put("name", "preset 映射项目");
        preset.put("sourceText", "第一幕：测试 preset。");
        preset.put("visualStyle", "preset:film-cinematic");
        preset.put("aspectRatio", "16:9");
        preset.put("targetDuration", 15);
        preset.put("language", "中文");
        JsonNode createdPreset = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(preset))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(createdPreset.path("project").path("visualStyle").asText()).isEqualTo("live-action");

        String longStyle = "长文本视觉风格前缀。" + "某".repeat(8900);
        Map<String, Object> longReq = new LinkedHashMap<>();
        longReq.put("name", "长文本风格项目");
        longReq.put("sourceText", "第一幕：测试长文本。");
        longReq.put("visualStyle", longStyle);
        longReq.put("aspectRatio", "16:9");
        longReq.put("targetDuration", 15);
        longReq.put("language", "中文");
        JsonNode createdLong = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(longReq))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(createdLong.path("project").path("visualStyle").asText().length()).isGreaterThan(8000);
    }

    @Test
    void workflowImageModelSettingsPersistClearAndDriveKeyframeModelName() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，准备在摊位边完成今天第一张写生。
                """, "gpt-4o-mini");
        createCapabilityModelConfig("image-default-v1", "image");
        createCapabilityModelConfig("image-override-v1", "image");

        Map<String, Object> initialSettings = new LinkedHashMap<>();
        initialSettings.put("defaultImageModel", "image-default-v1");
        initialSettings.put("overrides", Map.of(WorkflowModelKey.KEYFRAME_IMAGE, "image-override-v1"));
        JsonNode savedSettings = readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(initialSettings))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(savedSettings.path("defaultImageModel").asText()).isEqualTo("image-default-v1");
        assertThat(savedSettings.path("overrides").path(WorkflowModelKey.KEYFRAME_IMAGE).asText()).isEqualTo("image-override-v1");

        var configuredAggregate = scriptProjectService.require(projectId);
        assertThat(configuredAggregate.project.explicitImageModel).isEqualTo("image-default-v1");
        assertThat(scriptProjectService.parseOverrides(configuredAggregate.project.workflowModelOverrides))
                .containsEntry(WorkflowModelKey.KEYFRAME_IMAGE, "image-override-v1");

        Map<String, Object> updateScriptRequest = new LinkedHashMap<>();
        updateScriptRequest.put("refinedMarkdown", """
                # 古镇清晨

                阿满背着画箱来到古镇街口，准备在茶摊边完成今天的第一张写生。
                """);
        updateScriptRequest.put("structuredScript", buildManualStructuredScript());
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/script", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateScriptRequest))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode extractedCharacters = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/characters", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        String assetId = extractedCharacters.get(0).path("assetId").asText();

        JsonNode overriddenKeyframes = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate", projectId, assetId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(overriddenKeyframes.size()).isEqualTo(2);
        assertThat(overriddenKeyframes.get(0).path("modelName").asText()).isEqualTo("image-override-v1");

        Map<String, Object> clearOverrideSettings = new LinkedHashMap<>();
        clearOverrideSettings.put("defaultImageModel", "image-default-v1");
        clearOverrideSettings.put("overrides", Map.of(WorkflowModelKey.KEYFRAME_IMAGE, ""));
        JsonNode clearedSettings = readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(clearOverrideSettings))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(clearedSettings.path("overrides").has(WorkflowModelKey.KEYFRAME_IMAGE)).isFalse();

        var clearedOverrideAggregate = scriptProjectService.require(projectId);
        assertThat(scriptProjectService.parseOverrides(clearedOverrideAggregate.project.workflowModelOverrides))
                .doesNotContainKey(WorkflowModelKey.KEYFRAME_IMAGE);

        JsonNode defaultKeyframes = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate", projectId, assetId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(defaultKeyframes.size()).isEqualTo(2);
        assertThat(defaultKeyframes.get(0).path("modelName").asText()).isEqualTo("image-default-v1");

        Map<String, Object> clearDefaultSettings = new LinkedHashMap<>();
        clearDefaultSettings.put("defaultImageModel", "");
        clearDefaultSettings.put("overrides", Map.of());
        JsonNode clearedDefaultSettings = readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(clearDefaultSettings))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(clearedDefaultSettings.path("defaultImageModel").isNull()).isTrue();

        var clearedDefaultAggregate = scriptProjectService.require(projectId);
        assertThat(clearedDefaultAggregate.project.explicitImageModel).isNull();
        assertThat(scriptProjectService.parseOverrides(clearedDefaultAggregate.project.workflowModelOverrides)).isEmpty();
    }

    @Test
    void threeViewUsesWorkflowModelAndFailsWithoutPlaceholderWhenGatewayBreaks() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，准备在摊位边完成今天第一张写生。
                """, "gpt-4o-mini");
        createCapabilityModelConfig("image-three-default-v1", "image");
        createCapabilityModelConfig("image-three-override-v1", "image");

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("defaultImageModel", "image-three-default-v1");
        settings.put("overrides", Map.of(WorkflowModelKey.THREE_VIEW_IMAGE, "image-three-override-v1"));
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(settings))))
                .andExpect(status().isOk())
                .andReturn());

        Map<String, Object> updateScriptRequest = new LinkedHashMap<>();
        updateScriptRequest.put("refinedMarkdown", """
                # 古镇清晨

                阿满背着画箱来到古镇街口，准备在茶摊边完成今天的第一张写生。
                """);
        updateScriptRequest.put("structuredScript", buildManualStructuredScript());
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/script", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateScriptRequest))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode extractedCharacters = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/characters", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        String assetId = extractedCharacters.get(0).path("assetId").asText();

        Map<String, Object> updateAssetRequest = new LinkedHashMap<>();
        updateAssetRequest.put("visualPrompt", "cinematic character design, full body, strong identity consistency");
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/assets/{assetId}", projectId, assetId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateAssetRequest))))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode generatedThreeView = readSuccessData(mockMvc.perform(withScriptAuth(post(
                        "/api/v1/script-projects/{projectId}/assets/{assetId}/three-view/generate",
                        projectId,
                        assetId)))
                .andExpect(status().isOk())
                .andReturn());
        String successFileId = generatedThreeView.path("imageFileId").asText();
        assertThat(successFileId).isNotBlank();

        JsonNode assetsAfterSuccess = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/assets", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode updatedAsset = null;
        for (JsonNode item : assetsAfterSuccess) {
            if (assetId.equals(item.path("assetId").asText())) {
                updatedAsset = item;
                break;
            }
        }
        assertThat(updatedAsset).isNotNull();
        JsonNode verifiedUpdatedAsset = Objects.requireNonNull(updatedAsset);
        assertThat(verifiedUpdatedAsset.path("threeViewImageFileId").asText()).isEqualTo(successFileId);

        JsonNode detailAfterSuccess = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode aggregateAsset = null;
        for (JsonNode item : detailAfterSuccess.path("assets")) {
            if (assetId.equals(item.path("assetId").asText())) {
                aggregateAsset = item;
                break;
            }
        }
        if (aggregateAsset != null) {
            assertThat(aggregateAsset.path("threeViewImageFileId").asText()).isEqualTo(successFileId);
        }

        var aggregateAfterSuccess = scriptProjectService.require(projectId);
        StoredFileRecord successFile = aggregateAfterSuccess.files.stream()
                .filter(file -> successFileId.equals(file.fileId))
                .findFirst()
                .orElseThrow();
        assertThat(successFile.relativePath).contains("three-view/" + assetId + "/");
        assertThat(successFile.relativePath).endsWith(".png");

        doThrow(new RuntimeException("mock-image-downstream-error"))
                .when(providerHttpGateway)
                .generateImage(any(), anyString(), anyString(), anyMap(), any());

        JsonNode failedThreeView = readResponseTree(mockMvc.perform(withScriptAuth(post(
                        "/api/v1/script-projects/{projectId}/assets/{assetId}/three-view/generate",
                        projectId,
                        assetId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(failedThreeView.path("code").asInt()).isEqualTo(502);
        assertThat(failedThreeView.path("message").asText()).contains("调用图片模型失败");

        var aggregateAfterFailure = scriptProjectService.require(projectId);
        assertThat(aggregateAfterFailure.files.stream()
                .map(file -> file.relativePath == null ? "" : file.relativePath)
                .filter(path -> path.startsWith("three-view/" + assetId + "/"))
                .noneMatch(path -> path.endsWith(".svg"))).isTrue();
    }

    @Test
    void dubbingWorkflowStoresAudioAndSupportsRetryAfterModelFix() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，轻声自述今天一定要完成写生。
                """, "gpt-4o-mini");

        Map<String, Object> updateScriptRequest = new LinkedHashMap<>();
        updateScriptRequest.put("refinedMarkdown", """
                # 古镇清晨

                阿满背着画箱走进古镇街口，边走边低声给自己打气。
                """);
        updateScriptRequest.put("structuredScript", buildManualStructuredScript());
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/script", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateScriptRequest))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/shots/split", projectId)))
                .andExpect(status().isOk())
                .andReturn());

        createCapabilityModelConfig("tts-mock-v1", "tts");

        Map<String, Object> invalidSettings = new LinkedHashMap<>();
        invalidSettings.put("defaultTtsModel", "missing-tts-model");
        invalidSettings.put("dubbingVoice", "温和旁白");
        invalidSettings.put("dubbingLanguage", "zh-CN");
        invalidSettings.put("dubbingSpeed", 1.15d);
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(invalidSettings))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/dubbing/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode failedPipeline = waitForDubbingCompletion(projectId);
        assertThat(failedPipeline.path("projectStatus").asText()).isEqualTo("FAILED");
        assertThat(failedPipeline.path("dubbingFailedCount").asInt()).isEqualTo(1);
        assertThat(failedPipeline.path("dubbingReady").asBoolean()).isFalse();

        JsonNode failedTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/dubbing/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(failedTasks.size()).isEqualTo(1);
        assertThat(failedTasks.get(0).path("status").asText()).isEqualTo("FAILED");
        assertThat(failedTasks.get(0).path("errorMessage").asText()).contains("未命中已配置的配音模型");
        String dubbingTaskId = failedTasks.get(0).path("dubbingTaskId").asText();

        Map<String, Object> validSettings = new LinkedHashMap<>();
        validSettings.put("defaultTtsModel", "tts-mock-v1");
        validSettings.put("dubbingVoice", "温和旁白");
        validSettings.put("dubbingLanguage", "zh-CN");
        validSettings.put("dubbingSpeed", 1.15d);
        JsonNode savedSettings = readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(validSettings))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(savedSettings.path("defaultTtsModel").asText()).isEqualTo("tts-mock-v1");
        assertThat(savedSettings.path("dubbingVoice").asText()).isEqualTo("温和旁白");
        assertThat(savedSettings.path("dubbingLanguage").asText()).isEqualTo("zh-CN");

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/dubbing/tasks/{dubbingTaskId}/retry", projectId, dubbingTaskId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode completedPipeline = waitForDubbingCompletion(projectId);
        assertThat(completedPipeline.path("projectStatus").asText()).isEqualTo("SCRIPT_READY");
        assertThat(completedPipeline.path("dubbingReady").asBoolean()).isTrue();
        assertThat(completedPipeline.path("dubbingSuccessCount").asInt()).isEqualTo(1);

        JsonNode successTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/dubbing/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(successTasks.get(0).path("status").asText()).isEqualTo("SUCCESS");
        assertThat(successTasks.get(0).path("modelName").asText()).isEqualTo("tts-mock-v1");
        assertThat(successTasks.get(0).path("voiceName").asText()).isEqualTo("温和旁白");
        String audioFileId = successTasks.get(0).path("resultAudioFileId").asText();
        assertThat(audioFileId).isNotBlank();

        MvcResult audioPreview = mockMvc.perform(get("/api/v1/files/{fileId}", audioFileId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(audioPreview.getResponse().getContentType()).startsWith("audio/wav");
        assertThat(audioPreview.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void lipSyncWorkflowValidatesDependenciesStoresResultAndSupportsRetry() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，边走边低声练习今天的旁白。
                """, "gpt-4o-mini");

        Map<String, Object> updateScriptRequest = new LinkedHashMap<>();
        updateScriptRequest.put("refinedMarkdown", """
                # 古镇清晨

                阿满背着画箱走进古镇街口，边走边轻声自述今天一定要画好第一张写生。
                """);
        updateScriptRequest.put("structuredScript", buildManualStructuredScript());
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/script", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateScriptRequest))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/shots/split", projectId)))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode missingDependencyResponse = readResponseTree(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/lip-sync/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(missingDependencyResponse.path("code").asInt()).isEqualTo(400);
        assertThat(missingDependencyResponse.path("message").asText())
                .contains("缺少源视频")
                .contains("缺少配音音频");

        JsonNode extractedCharacters = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/characters", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedBackgrounds = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/backgrounds", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedProps = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/props", projectId)))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode assets = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/assets", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        Set<String> requiredAssetIds = collectShotAssetIds(readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/shots", projectId)))
                .andExpect(status().isOk())
                .andReturn()));
        assertThat(extractedCharacters.size() + extractedBackgrounds.size() + extractedProps.size()).isEqualTo(assets.size());

        for (JsonNode asset : assets) {
            String assetId = asset.path("assetId").asText();
            if (!requiredAssetIds.contains(assetId)) {
                continue;
            }
            JsonNode generatedKeyframes = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate", projectId, assetId)))
                    .andExpect(status().isOk())
                    .andReturn());
            String confirmedKeyframeId = generatedKeyframes.get(0).path("keyframeId").asText();
            readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/keyframes/{keyframeId}/confirm", projectId, confirmedKeyframeId)))
                    .andExpect(status().isOk())
                    .andReturn());
        }

        createCapabilityModelConfig("tts-mock-v1", "tts");
        Map<String, Object> ttsSettings = new LinkedHashMap<>();
        ttsSettings.put("defaultTtsModel", "tts-mock-v1");
        ttsSettings.put("dubbingVoice", "温和旁白");
        ttsSettings.put("dubbingLanguage", "zh-CN");
        ttsSettings.put("dubbingSpeed", 1.0d);
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(ttsSettings))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/video/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode videoPipeline = waitForVideoCompletion(projectId);
        assertThat(videoPipeline.path("videoReady").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/dubbing/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode dubbingPipeline = waitForDubbingCompletion(projectId);
        assertThat(dubbingPipeline.path("projectStatus").asText()).isEqualTo("DUBBING_READY");
        assertThat(dubbingPipeline.path("dubbingReady").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/lip-sync/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode lipSyncPipeline = waitForLipSyncCompletion(projectId);
        assertThat(lipSyncPipeline.path("projectStatus").asText()).isEqualTo("LIP_SYNC_READY");
        assertThat(lipSyncPipeline.path("lipSyncReady").asBoolean()).isTrue();
        assertThat(lipSyncPipeline.path("lipSyncSuccessCount").asInt()).isEqualTo(1);

        JsonNode lipSyncTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/lip-sync/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(lipSyncTasks.size()).isEqualTo(1);
        JsonNode lipSyncTask = lipSyncTasks.get(0);
        assertThat(lipSyncTask.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(lipSyncTask.path("sourceVideoFileId").asText()).isNotBlank();
        assertThat(lipSyncTask.path("sourceAudioFileId").asText()).isNotBlank();
        String lipSyncTaskId = lipSyncTask.path("lipSyncTaskId").asText();
        String lipSyncVideoFileId = lipSyncTask.path("resultVideoFileId").asText();
        assertThat(lipSyncVideoFileId).isNotBlank();

        MvcResult lipSyncPreview = mockMvc.perform(get("/api/v1/files/{fileId}", lipSyncVideoFileId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(lipSyncPreview.getResponse().getContentType()).startsWith("video/mp4");
        assertThat(lipSyncPreview.getResponse().getContentAsByteArray()).isNotEmpty();

        var aggregate = scriptProjectService.require(projectId);
        var sourceAudio = scriptProjectService.findFile(aggregate, lipSyncTask.path("sourceAudioFileId").asText());
        assertThat(sourceAudio).isNotNull();
        Files.deleteIfExists(localAssetFileService.resolveStoredFile(sourceAudio));

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/lip-sync/tasks/{lipSyncTaskId}/retry", projectId, lipSyncTaskId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode failedLipSyncPipeline = waitForLipSyncCompletion(projectId);
        assertThat(failedLipSyncPipeline.path("projectStatus").asText()).isEqualTo("FAILED");
        assertThat(failedLipSyncPipeline.path("lipSyncFailedCount").asInt()).isEqualTo(1);

        JsonNode failedLipSyncTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/lip-sync/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(failedLipSyncTasks.get(0).path("errorMessage").asText()).contains("配音音频文件已丢失");

        JsonNode dubbingTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/dubbing/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        String dubbingTaskId = dubbingTasks.get(0).path("dubbingTaskId").asText();
        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/dubbing/tasks/{dubbingTaskId}/retry", projectId, dubbingTaskId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode restoredDubbingPipeline = waitForDubbingCompletion(projectId);
        assertThat(restoredDubbingPipeline.path("dubbingReady").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/lip-sync/tasks/{lipSyncTaskId}/retry", projectId, lipSyncTaskId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode retriedLipSyncPipeline = waitForLipSyncCompletion(projectId);
        assertThat(retriedLipSyncPipeline.path("projectStatus").asText()).isEqualTo("LIP_SYNC_READY");
        assertThat(retriedLipSyncPipeline.path("lipSyncReady").asBoolean()).isTrue();
    }

    @Test
    void finalCompositionWorkflowStoresMediaPrefersLipSyncAndFallsBackToVideo() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，边走边低声练习今天的旁白。
                """, "gpt-4o-mini");

        Map<String, Object> updateScriptRequest = new LinkedHashMap<>();
        updateScriptRequest.put("refinedMarkdown", """
                # 古镇清晨

                阿满背着画箱走进古镇街口，边走边轻声自述今天一定要画好第一张写生。
                """);
        updateScriptRequest.put("structuredScript", buildManualStructuredScript());
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/script", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateScriptRequest))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/shots/split", projectId)))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode missingDependencyResponse = readResponseTree(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/final-composition/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(missingDependencyResponse.path("code").asInt()).isEqualTo(400);
        assertThat(missingDependencyResponse.path("message").asText()).contains("缺少可用于成片编排的结果");

        JsonNode missingExportDependencyResponse = readResponseTree(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/export-package/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(missingExportDependencyResponse.path("code").asInt()).isEqualTo(400);
        assertThat(missingExportDependencyResponse.path("message").asText()).contains("缺少项目级成片");

        JsonNode extractedCharacters = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/characters", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedBackgrounds = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/backgrounds", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedProps = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/props", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode assets = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/assets", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        Set<String> requiredAssetIds = collectShotAssetIds(readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/shots", projectId)))
                .andExpect(status().isOk())
                .andReturn()));
        assertThat(extractedCharacters.size() + extractedBackgrounds.size() + extractedProps.size()).isEqualTo(assets.size());
        for (JsonNode asset : assets) {
            String assetId = asset.path("assetId").asText();
            if (!requiredAssetIds.contains(assetId)) {
                continue;
            }
            JsonNode generatedKeyframes = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate", projectId, assetId)))
                    .andExpect(status().isOk())
                    .andReturn());
            String confirmedKeyframeId = generatedKeyframes.get(0).path("keyframeId").asText();
            readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/keyframes/{keyframeId}/confirm", projectId, confirmedKeyframeId)))
                    .andExpect(status().isOk())
                    .andReturn());
        }

        createCapabilityModelConfig("tts-mock-v1", "tts");
        Map<String, Object> ttsSettings = new LinkedHashMap<>();
        ttsSettings.put("defaultTtsModel", "tts-mock-v1");
        ttsSettings.put("dubbingVoice", "温和旁白");
        ttsSettings.put("dubbingLanguage", "zh-CN");
        ttsSettings.put("dubbingSpeed", 1.0d);
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(ttsSettings))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/video/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode videoPipeline = waitForVideoCompletion(projectId);
        assertThat(videoPipeline.path("videoReady").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/dubbing/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode dubbingPipeline = waitForDubbingCompletion(projectId);
        assertThat(dubbingPipeline.path("dubbingReady").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/lip-sync/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode lipSyncPipeline = waitForLipSyncCompletion(projectId);
        assertThat(lipSyncPipeline.path("projectStatus").asText()).isEqualTo("LIP_SYNC_READY");

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/final-composition/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode finalPipeline = waitForFinalCompositionCompletion(projectId);
        assertThat(finalPipeline.path("projectStatus").asText()).isEqualTo("FINAL_COMPOSITION_READY");
        assertThat(finalPipeline.path("finalCompositionReady").asBoolean()).isTrue();
        assertThat(finalPipeline.path("latestRun").path("pipelineType").asText()).isEqualTo("FINAL_COMPOSITION");

        JsonNode finalTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/final-composition/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(finalTasks.size()).isEqualTo(1);
        JsonNode finalTask = finalTasks.get(0);
        assertThat(finalTask.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(finalTask.path("inputSegments").get(0).path("sourceType").asText()).isEqualTo("LIP_SYNC");
        String finalTaskId = finalTask.path("finalCompositionTaskId").asText();
        String finalVideoFileId = finalTask.path("resultVideoFileId").asText();
        assertThat(finalVideoFileId).isNotBlank();

        MvcResult finalPreview = mockMvc.perform(get("/api/v1/files/{fileId}", finalVideoFileId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(finalPreview.getResponse().getContentType()).startsWith("video/mp4");
        assertThat(finalPreview.getResponse().getContentAsByteArray()).isNotEmpty();

        var aggregate = scriptProjectService.require(projectId);
        var finalVideo = scriptProjectService.findFile(aggregate, finalVideoFileId);
        assertThat(finalVideo).isNotNull();
        assertThat(finalVideo.storageProvider.name()).isEqualTo("LOCAL");
        assertThat(finalVideo.bucketName).isNotBlank();
        assertThat(finalVideo.objectKey).contains("final-composition/");
        assertThat(finalVideo.publicUrl).contains("/api/v1/files/");

        JsonNode lipSyncTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/lip-sync/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        String lipSyncVideoFileId = lipSyncTasks.get(0).path("resultVideoFileId").asText();
        var lipSyncVideo = scriptProjectService.findFile(aggregate, lipSyncVideoFileId);
        assertThat(lipSyncVideo).isNotNull();
        Files.deleteIfExists(localAssetFileService.resolveStoredFile(lipSyncVideo));

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/final-composition/tasks/{finalCompositionTaskId}/retry", projectId, finalTaskId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode retriedPipeline = waitForFinalCompositionCompletion(projectId);
        assertThat(retriedPipeline.path("projectStatus").asText()).isEqualTo("FINAL_COMPOSITION_READY");

        JsonNode retriedTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/final-composition/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(retriedTasks.get(0).path("inputSegments").get(0).path("sourceType").asText()).isEqualTo("VIDEO");

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/export-package/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode exportPipeline = waitForExportPackageCompletion(projectId);
        assertThat(exportPipeline.path("projectStatus").asText()).isEqualTo("EXPORT_PACKAGE_READY");
        assertThat(exportPipeline.path("exportPackageReady").asBoolean()).isTrue();
        assertThat(exportPipeline.path("latestRun").path("pipelineType").asText()).isEqualTo("EXPORT_PACKAGE");

        JsonNode exportTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/export-package/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(exportTasks.size()).isEqualTo(1);
        JsonNode exportTask = exportTasks.get(0);
        assertThat(exportTask.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(exportTask.path("manifestFileId").asText()).isNotBlank();
        assertThat(exportTask.path("resultArchiveFileId").asText()).isNotBlank();
        assertThat(exportTask.path("archiveStorageProvider").asText()).isEqualTo("LOCAL");
        assertThat(exportTask.path("archiveBucketName").asText()).isNotBlank();
        assertThat(exportTask.path("archiveObjectKey").asText()).contains("export-package/");
        assertThat(exportTask.path("archivePublicUrl").asText()).contains("/api/v1/files/");
        assertThat(exportTask.path("manifestStorageProvider").asText()).isEqualTo("LOCAL");

        MvcResult exportArchive = mockMvc.perform(get("/api/v1/files/{fileId}/download", exportTask.path("resultArchiveFileId").asText()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(exportArchive.getResponse().getContentType()).startsWith("application/zip");

        Map<String, byte[]> archiveEntries = unzipEntries(exportArchive.getResponse().getContentAsByteArray());
        assertThat(archiveEntries).containsKey("manifest.json");
        String mediaEntryName = archiveEntries.keySet().stream()
                .filter(name -> name.startsWith("media/") && name.endsWith(".mp4"))
                .findFirst()
                .orElse(null);
        assertThat(mediaEntryName).isNotBlank();
        JsonNode manifest = objectMapper.readTree(archiveEntries.get("manifest.json"));
        assertThat(manifest.path("project").path("projectName").asText()).isEqualTo("古镇清晨项目");
        assertThat(manifest.path("files").isArray()).isTrue();
        assertThat(manifest.path("shots").size()).isEqualTo(1);
    }

    @Test
    void exportPackageWorkflowSupportsFailureRetryAndStoredResultMetadata() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，边走边低声练习今天的旁白。
                """, "gpt-4o-mini");

        Map<String, Object> updateScriptRequest = new LinkedHashMap<>();
        updateScriptRequest.put("refinedMarkdown", """
                # 古镇清晨

                阿满背着画箱走进古镇街口，边走边轻声自述今天一定要画好第一张写生。
                """);
        updateScriptRequest.put("structuredScript", buildManualStructuredScript());
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/script", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(updateScriptRequest))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/shots/split", projectId)))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode extractedCharacters = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/characters", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedBackgrounds = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/backgrounds", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedProps = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/extract/props", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode assets = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/assets", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        Set<String> requiredAssetIds = collectShotAssetIds(readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/shots", projectId)))
                .andExpect(status().isOk())
                .andReturn()));
        assertThat(extractedCharacters.size() + extractedBackgrounds.size() + extractedProps.size()).isEqualTo(assets.size());
        for (JsonNode asset : assets) {
            String assetId = asset.path("assetId").asText();
            if (!requiredAssetIds.contains(assetId)) {
                continue;
            }
            JsonNode generatedKeyframes = readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate", projectId, assetId)))
                    .andExpect(status().isOk())
                    .andReturn());
            String confirmedKeyframeId = generatedKeyframes.get(0).path("keyframeId").asText();
            readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/keyframes/{keyframeId}/confirm", projectId, confirmedKeyframeId)))
                    .andExpect(status().isOk())
                    .andReturn());
        }

        createCapabilityModelConfig("tts-mock-v1", "tts");
        Map<String, Object> ttsSettings = new LinkedHashMap<>();
        ttsSettings.put("defaultTtsModel", "tts-mock-v1");
        ttsSettings.put("dubbingVoice", "温和旁白");
        ttsSettings.put("dubbingLanguage", "zh-CN");
        ttsSettings.put("dubbingSpeed", 1.0d);
        readSuccessData(mockMvc.perform(
                        withScriptAuth(put("/api/v1/script-projects/{projectId}/model-settings", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(ttsSettings))))
                .andExpect(status().isOk())
                .andReturn());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/video/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode videoPipeline = waitForVideoCompletion(projectId);
        assertThat(videoPipeline.path("videoReady").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/dubbing/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode dubbingPipeline = waitForDubbingCompletion(projectId);
        assertThat(dubbingPipeline.path("dubbingReady").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/lip-sync/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode lipSyncPipeline = waitForLipSyncCompletion(projectId);
        assertThat(lipSyncPipeline.path("projectStatus").asText()).isEqualTo("LIP_SYNC_READY");

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/final-composition/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode finalPipeline = waitForFinalCompositionCompletion(projectId);
        assertThat(finalPipeline.path("projectStatus").asText()).isEqualTo("FINAL_COMPOSITION_READY");

        JsonNode finalTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/final-composition/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(finalTasks.size()).isEqualTo(1);
        String finalCompositionTaskId = finalTasks.get(0).path("finalCompositionTaskId").asText();
        String finalVideoFileId = finalTasks.get(0).path("resultVideoFileId").asText();
        assertThat(finalVideoFileId).isNotBlank();

        boolean[] failArchiveOnce = {true};
        doAnswer(invocation -> {
            String relativePath = invocation.getArgument(1);
            if (failArchiveOnce[0]
                    && relativePath != null
                    && relativePath.startsWith("export-package/")
                    && relativePath.endsWith("/package.zip")) {
                failArchiveOnce[0] = false;
                throw new IllegalStateException("模拟导出包归档写入失败");
            }
            return invocation.callRealMethod();
        }).when(localAssetFileService).storeBytes(anyString(), anyString(), anyString(), any());

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/export-package/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode failedExportPipeline = waitForExportPackageCompletion(projectId);
        assertThat(failedExportPipeline.path("projectStatus").asText()).isEqualTo("FAILED");
        assertThat(failedExportPipeline.path("exportPackageFailedCount").asInt()).isEqualTo(1);
        assertThat(failedExportPipeline.path("exportPackageReady").asBoolean()).isFalse();

        JsonNode failedExportTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/export-package/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(failedExportTasks.size()).isEqualTo(1);
        JsonNode failedExportTask = failedExportTasks.get(0);
        String exportPackageTaskId = failedExportTask.path("exportPackageTaskId").asText();
        assertThat(failedExportTask.path("status").asText()).isEqualTo("FAILED");
        assertThat(failedExportTask.path("errorMessage").asText()).contains("导出包生成失败");
        assertThat(failedExportTask.path("manifestFileId").isNull()).isTrue();
        assertThat(failedExportTask.path("resultArchiveFileId").isNull()).isTrue();
        assertThat(failedExportTask.path("archiveStorageProvider").isNull()).isTrue();
        assertThat(failedExportTask.path("manifestStorageProvider").isNull()).isTrue();

        readSuccessData(mockMvc.perform(withScriptAuth(post("/api/v1/script-projects/{projectId}/export-package/tasks/{exportPackageTaskId}/retry", projectId, exportPackageTaskId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode recoveredExportPipeline = waitForExportPackageCompletion(projectId);
        assertThat(recoveredExportPipeline.path("projectStatus").asText()).isEqualTo("EXPORT_PACKAGE_READY");
        assertThat(recoveredExportPipeline.path("exportPackageReady").asBoolean()).isTrue();
        assertThat(recoveredExportPipeline.path("exportPackageSuccessCount").asInt()).isEqualTo(1);

        JsonNode recoveredExportTasks = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/export-package/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(recoveredExportTasks.size()).isEqualTo(1);
        JsonNode recoveredExportTask = recoveredExportTasks.get(0);
        assertThat(recoveredExportTask.path("exportPackageTaskId").asText()).isEqualTo(exportPackageTaskId);
        assertThat(recoveredExportTask.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(recoveredExportTask.path("retryCount").asInt()).isEqualTo(1);
        assertThat(recoveredExportTask.path("errorMessage").isNull()).isTrue();
        assertThat(recoveredExportTask.path("sourceFinalCompositionTaskId").asText()).isEqualTo(finalCompositionTaskId);
        assertThat(recoveredExportTask.path("sourceFinalVideoFileId").asText()).isEqualTo(finalVideoFileId);
        assertThat(recoveredExportTask.path("manifestFileId").asText()).isNotBlank();
        assertThat(recoveredExportTask.path("resultArchiveFileId").asText()).isNotBlank();
        assertThat(recoveredExportTask.path("archiveStorageProvider").asText()).isEqualTo("LOCAL");
        assertThat(recoveredExportTask.path("archiveBucketName").asText()).isNotBlank();
        assertThat(recoveredExportTask.path("archiveObjectKey").asText()).contains("export-package/");
        assertThat(recoveredExportTask.path("archivePublicUrl").asText()).contains("/api/v1/files/");
        assertThat(recoveredExportTask.path("manifestStorageProvider").asText()).isEqualTo("LOCAL");
        assertThat(recoveredExportTask.path("manifestBucketName").asText()).isNotBlank();
        assertThat(recoveredExportTask.path("manifestObjectKey").asText()).contains("export-package/");
        assertThat(recoveredExportTask.path("manifestPublicUrl").asText()).contains("/api/v1/files/");

        MvcResult recoveredArchive = mockMvc.perform(get("/api/v1/files/{fileId}/download", recoveredExportTask.path("resultArchiveFileId").asText()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(recoveredArchive.getResponse().getContentType()).startsWith("application/zip");

        Map<String, byte[]> archiveEntries = unzipEntries(recoveredArchive.getResponse().getContentAsByteArray());
        assertThat(archiveEntries).containsKey("manifest.json");
        assertThat(archiveEntries.keySet()).anyMatch(name -> name.startsWith("media/") && name.endsWith(".mp4"));
        JsonNode manifest = objectMapper.readTree(archiveEntries.get("manifest.json"));
        assertThat(manifest.path("exportPackageTaskId").asText()).isEqualTo(exportPackageTaskId);
        assertThat(manifest.path("sourceFinalCompositionTaskId").asText()).isEqualTo(finalCompositionTaskId);
        assertThat(manifest.path("project").path("projectId").asText()).isEqualTo(projectId);
        assertThat(manifest.path("files").isArray()).isTrue();
        assertThat(manifest.path("shots").size()).isEqualTo(1);
    }

    @Test
    void contentReviewWorkflowSupportsRejectResubmitApproveAndAuditLogs() throws Exception {
        String projectId = createTextProject("""
                第一幕：清晨，古镇街口。
                阿满背着画箱穿过薄雾，准备提交最终交付。
                """, "gpt-4o-mini");

        JsonNode blockedSubmit = readResponseTree(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects/{projectId}/content-review/submit", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of("comment", "请审核第一版交付")))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(blockedSubmit.path("code").asInt()).isEqualTo(400);
        assertThat(blockedSubmit.path("message").asText()).contains("导出包");

        seedExportReadyPackage(projectId);

        JsonNode submitted = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects/{projectId}/content-review/submit", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of("comment", "请审核第一版交付")))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(submitted.path("status").asText()).isEqualTo("PENDING");
        assertThat(submitted.path("resubmitCount").asInt()).isEqualTo(0);
        assertThat(submitted.path("currentReviewId").asText()).isNotBlank();
        assertThat(submitted.path("records")).hasSize(1);
        String firstReviewId = submitted.path("currentReviewId").asText();

        JsonNode pendingPipeline = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(pendingPipeline.path("contentReviewStatus").asText()).isEqualTo("PENDING");

        JsonNode selfApproveBlocked = readResponseTree(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects/{projectId}/content-review/approve", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of("comment", "自己不能通过")))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(selfApproveBlocked.path("code").asInt()).isEqualTo(403);
        assertThat(selfApproveBlocked.path("message").asText()).contains("不能审核自己提交的项目");

        JsonNode rejected = readSuccessData(mockMvc.perform(
                        withScriptAuth(
                                post("/api/v1/script-projects/{projectId}/content-review/reject", projectId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsBytes(Map.of("comment", "请补充片头字幕与导出说明"))),
                                "admin-reviewer"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.path("latestReviewComment").asText()).contains("片头字幕");
        assertThat(rejected.path("records")).hasSize(1);
        assertThat(rejected.path("records").get(0).path("status").asText()).isEqualTo("REJECTED");

        JsonNode rejectedStatus = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/content-review", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(rejectedStatus.path("status").asText()).isEqualTo("REJECTED");
        assertThat(rejectedStatus.path("canSubmit").asBoolean()).isTrue();

        JsonNode resubmitted = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects/{projectId}/content-review/submit", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of("comment", "已按意见补充片头字幕，请再次审核")))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(resubmitted.path("status").asText()).isEqualTo("PENDING");
        assertThat(resubmitted.path("resubmitCount").asInt()).isEqualTo(1);
        assertThat(resubmitted.path("records")).hasSize(2);
        assertThat(resubmitted.path("currentReviewId").asText()).isNotEqualTo(firstReviewId);

        JsonNode approved = readSuccessData(mockMvc.perform(
                        withScriptAuth(
                                post("/api/v1/script-projects/{projectId}/content-review/approve", projectId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsBytes(Map.of("comment", "内容符合交付要求，可以发布"))),
                                "admin-reviewer"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("latestReviewComment").asText()).contains("可以发布");
        assertThat(approved.path("records")).hasSize(2);
        assertThat(approved.path("records").get(0).path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("records").get(1).path("status").asText()).isEqualTo("REJECTED");

        JsonNode approvedPipeline = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(approvedPipeline.path("contentReviewStatus").asText()).isEqualTo("APPROVED");
        assertThat(approvedPipeline.path("reviewResubmitCount").asInt()).isEqualTo(1);

        JsonNode auditLogs = readSuccessData(mockMvc.perform(
                        withScriptAuth(get("/api/v1/audit-logs")
                                .param("entityType", "SCRIPT_PROJECT")
                                .param("entityId", projectId), "admin-reviewer"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(auditLogs.isArray()).isTrue();
        assertThat(jsonTextSet(auditLogs, "action")).contains(
                "PROJECT_REVIEW_SUBMITTED",
                "PROJECT_REVIEW_REJECTED",
                "PROJECT_REVIEW_RESUBMITTED",
                "PROJECT_REVIEW_APPROVED"
        );
    }

    private String createTextProject(String sourceText, String explicitTextModel) throws Exception {
        createTextModelConfig("gpt-4o-mini");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "古镇清晨项目");
        request.put("sourceText", sourceText);
        request.put("visualStyle", "电影感写实");
        request.put("aspectRatio", "16:9");
        request.put("targetDuration", 12);
        request.put("language", "zh-CN");
        if (explicitTextModel != null) {
            request.put("explicitTextModel", explicitTextModel);
        }

        JsonNode createData = readSuccessData(mockMvc.perform(
                        withScriptAuth(post("/api/v1/script-projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(createData.path("project").path("sourceType").asText()).isEqualTo("text");
        return createData.path("project").path("projectId").asText();
    }

    private void seedExportReadyPackage(String projectId) {
        var aggregate = scriptProjectService.require(projectId);
        StoredFileRecord manifestFile = localAssetFileService.storeJson(
                projectId,
                "export-package/mock/manifest.json",
                Map.of("projectId", projectId, "seed", true)
        );
        StoredFileRecord archiveFile = localAssetFileService.storeBytes(
                projectId,
                "export-package/mock/package.zip",
                "application/zip",
                new byte[]{1, 2, 3}
        );
        scriptProjectService.upsertFile(aggregate, manifestFile);
        scriptProjectService.upsertFile(aggregate, archiveFile);

        ExportPackageTask task = new ExportPackageTask();
        task.exportPackageTaskId = "pkg-seed-" + projectId;
        task.projectId = projectId;
        task.status = ExportPackageTaskStatus.SUCCESS;
        task.manifestFileId = manifestFile.fileId;
        task.resultArchiveFileId = archiveFile.fileId;
        task.manifestStorageProvider = manifestFile.storageProvider;
        task.manifestBucketName = manifestFile.bucketName;
        task.manifestObjectKey = manifestFile.objectKey;
        task.manifestPublicUrl = manifestFile.publicUrl;
        task.archiveStorageProvider = archiveFile.storageProvider;
        task.archiveBucketName = archiveFile.bucketName;
        task.archiveObjectKey = archiveFile.objectKey;
        task.archivePublicUrl = archiveFile.publicUrl;
        task.finishedAt = Instant.now();

        aggregate.exportPackageTasks.clear();
        aggregate.exportPackageTasks.add(task);
        aggregate.project.status = ProjectStatus.EXPORT_PACKAGE_READY;
        scriptProjectService.save(aggregate);
    }

    private void createTextModelConfig(String modelName) throws Exception {
        createCapabilityModelConfig(modelName, "text");
    }

    private void createCapabilityModelConfig(String modelName, String capability) throws Exception {
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
                .andExpect(status().isOk())
                .andReturn());
        String connectionId = connection.path("id").asText();

        Map<String, Object> modelRequest = new LinkedHashMap<>();
        modelRequest.put("name", "Test Text Model");
        modelRequest.put("provider", "openai");
        modelRequest.put("modelName", modelName);
        modelRequest.put("connectionId", connectionId);
        modelRequest.put("enabled", true);
        modelRequest.put("metadata", Map.of("capabilities", List.of(capability)));
        readSuccessData(mockMvc.perform(post("/api/v1/models")
                        .header("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
                        .header("x-aigc-token", TEST_ACCESS_TOKEN)
                        .header("x-user-id", "integration-test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(modelRequest)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private Map<String, Object> buildManualStructuredScript() {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("id", "scene-1");
        scene.put("title", "古镇街口");
        scene.put("location", "古镇街口");
        scene.put("time", "清晨");
        scene.put("atmosphere", "薄雾、安静、微暖");
        scene.put("summary", "阿满背着画箱走进古镇街口，准备开始写生。");

        Map<String, Object> mainCharacter = new LinkedHashMap<>();
        mainCharacter.put("id", "char-1");
        mainCharacter.put("name", "阿满");
        mainCharacter.put("description", "年轻画师，背着画箱，动作轻快但专注。");
        mainCharacter.put("tags", List.of("主角", "画师"));

        Map<String, Object> backgroundCharacter = new LinkedHashMap<>();
        backgroundCharacter.put("id", "char-2");
        backgroundCharacter.put("name", "路人乙");
        backgroundCharacter.put("description", "只是远景陪衬人物，不参与主要动作。");
        backgroundCharacter.put("tags", List.of("路人"));

        Map<String, Object> background = new LinkedHashMap<>();
        background.put("id", "bg-1");
        background.put("name", "古镇街口");
        background.put("description", "青石板路、木质店招、清晨薄雾与茶摊热气。");
        background.put("sceneId", "scene-1");

        Map<String, Object> mainProp = new LinkedHashMap<>();
        mainProp.put("id", "prop-1");
        mainProp.put("name", "画箱");
        mainProp.put("description", "木制画箱，肩背式，是阿满写生的核心道具。");
        mainProp.put("tags", List.of("核心道具"));

        Map<String, Object> unusedProp = new LinkedHashMap<>();
        unusedProp.put("id", "prop-2");
        unusedProp.put("name", "路牌");
        unusedProp.put("description", "街角木制路牌，只在远景中点缀画面。");
        unusedProp.put("tags", List.of("环境道具"));

        Map<String, Object> segment = new LinkedHashMap<>();
        segment.put("id", "segment-1");
        segment.put("title", "清晨入场");
        segment.put("scriptText", "阿满背着画箱穿过古镇街口，停在晨雾与茶摊热气之间。");
        segment.put("actionSummary", "主角入场并确定写生位置。");
        segment.put("cameraMovement", "中景跟拍后缓慢推近。");
        segment.put("characterIds", List.of("char-1"));
        segment.put("backgroundIds", List.of("bg-1"));
        segment.put("propIds", List.of("prop-1"));

        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("title", "古镇清晨");
        structured.put("summary", "阿满在古镇清晨展开写生，画面聚焦她与画箱、街口环境的关系。");
        structured.put("characters", List.of(mainCharacter, backgroundCharacter));
        structured.put("backgrounds", List.of(background));
        structured.put("props", List.of(mainProp, unusedProp));
        structured.put("scenes", List.of(scene));
        structured.put("segments", List.of(segment));
        return structured;
    }

    private Set<String> collectShotAssetIds(JsonNode shots) {
        Set<String> ids = new LinkedHashSet<>();
        for (JsonNode shot : shots) {
            addTextValues(ids, shot.path("characterRefs"));
            addTextValues(ids, shot.path("backgroundRefs"));
            addTextValues(ids, shot.path("propRefs"));
        }
        return ids;
    }

    private Set<String> jsonTextSet(JsonNode arrayNode, String fieldName) {
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode item : arrayNode) {
            String value = item.path(fieldName).asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private int countSelected(JsonNode keyframes) {
        int count = 0;
        for (JsonNode keyframe : keyframes) {
            if (keyframe.path("selected").asBoolean()) {
                count++;
            }
        }
        return count;
    }

    private Set<String> selectedAssetIds(JsonNode keyframes) {
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode keyframe : keyframes) {
            if (keyframe.path("selected").asBoolean()) {
                values.add(keyframe.path("assetId").asText());
            }
        }
        return values;
    }

    private void addTextValues(Set<String> values, JsonNode arrayNode) {
        for (JsonNode item : arrayNode) {
            String value = item.asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
    }

    private JsonNode waitForVideoCompletion(String projectId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            latest = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                    .andExpect(status().isOk())
                    .andReturn());
            if (latest.path("queuedCount").asInt() == 0 && latest.path("runningCount").asInt() == 0) {
                return latest;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("视频任务未在超时时间内完成: " + latest);
    }

    private JsonNode waitForDubbingCompletion(String projectId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            latest = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                    .andExpect(status().isOk())
                    .andReturn());
            if (latest.path("dubbingQueuedCount").asInt() == 0 && latest.path("dubbingRunningCount").asInt() == 0) {
                return latest;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("配音任务未在超时时间内完成: " + latest);
    }

    private JsonNode waitForLipSyncCompletion(String projectId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            latest = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                    .andExpect(status().isOk())
                    .andReturn());
            if (latest.path("lipSyncQueuedCount").asInt() == 0 && latest.path("lipSyncRunningCount").asInt() == 0) {
                return latest;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("口型同步任务未在超时时间内完成: " + latest);
    }

    private JsonNode waitForFinalCompositionCompletion(String projectId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            latest = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                    .andExpect(status().isOk())
                    .andReturn());
            if (latest.path("finalCompositionQueuedCount").asInt() == 0 && latest.path("finalCompositionRunningCount").asInt() == 0) {
                return latest;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("成片任务未在超时时间内完成: " + latest);
    }

    private JsonNode waitForExportPackageCompletion(String projectId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            latest = readSuccessData(mockMvc.perform(withScriptAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                    .andExpect(status().isOk())
                    .andReturn());
            if (latest.path("exportPackageQueuedCount").asInt() == 0 && latest.path("exportPackageRunningCount").asInt() == 0) {
                return latest;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("导出包任务未在超时时间内完成: " + latest);
    }

    private Map<String, byte[]> unzipEntries(byte[] archiveBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream inputStream = new ZipInputStream(new java.io.ByteArrayInputStream(archiveBytes))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                entries.put(entry.getName(), inputStream.readAllBytes());
                inputStream.closeEntry();
            }
        }
        return entries;
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
        return withScriptAuth(builder, "integration-test-user");
    }

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T withScriptAuth(T builder, String userId) {
        return builder
                .header("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
                .header("x-aigc-token", TEST_ACCESS_TOKEN)
                .header("x-user-id", userId)
                .header("x-user-name", userId);
    }

    private static byte[] createDocx(String... paragraphs) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                writeZipEntry(zipOutputStream, "[Content_Types].xml", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                          <Default Extension="xml" ContentType="application/xml"/>
                          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                          <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                          <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                        </Types>
                        """);
                writeZipEntry(zipOutputStream, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                        </Relationships>
                        """);
                writeZipEntry(zipOutputStream, "word/document.xml", documentXml(paragraphs));
                writeZipEntry(zipOutputStream, "docProps/core.xml", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                            xmlns:dc="http://purl.org/dc/elements/1.1/"
                            xmlns:dcterms="http://purl.org/dc/terms/"
                            xmlns:dcmitype="http://purl.org/dc/dcmitype/"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                          <dc:title>workflow-test</dc:title>
                          <dc:creator>Codex</dc:creator>
                          <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
                          <dcterms:created xsi:type="dcterms:W3CDTF">2026-03-27T06:35:00Z</dcterms:created>
                          <dcterms:modified xsi:type="dcterms:W3CDTF">2026-03-27T06:35:00Z</dcterms:modified>
                        </cp:coreProperties>
                        """);
                writeZipEntry(zipOutputStream, "docProps/app.xml", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
                            xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                          <Application>Codex</Application>
                        </Properties>
                        """);
            }
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("构造测试 DOCX 失败", ex);
        }
    }

    private static void writeZipEntry(ZipOutputStream outputStream, String name, String content) throws IOException {
        outputStream.putNextEntry(new ZipEntry(name));
        outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        outputStream.closeEntry();
    }

    private static String documentXml(String... paragraphs) {
        List<String> xmlParagraphs = new ArrayList<>();
        for (String paragraph : paragraphs) {
            xmlParagraphs.add("<w:p><w:r><w:t>" + escapeXml(paragraph) + "</w:t></w:r></w:p>");
        }
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas"
                    xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                    xmlns:o="urn:schemas-microsoft-com:office:office"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
                    xmlns:v="urn:schemas-microsoft-com:vml"
                    xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing"
                    xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                    xmlns:w10="urn:schemas-microsoft-com:office:word"
                    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
                    xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup"
                    xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk"
                    xmlns:wne="http://schemas.microsoft.com/office/2006/wordml"
                    xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
                    mc:Ignorable="w14 w15 wp14">
                  <w:body>
                    %s
                    <w:sectPr>
                      <w:pgSz w:w="11906" w:h="16838"/>
                      <w:pgMar w:top="1440" w:right="1800" w:bottom="1440" w:left="1800" w:header="851" w:footer="992" w:gutter="0"/>
                    </w:sectPr>
                  </w:body>
                </w:document>
                """.formatted(String.join("\n", xmlParagraphs));
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static Path createDataDir() {
        try {
            return Files.createTempDirectory("aigc-server-it-");
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
