package com.example.aigc;

import com.example.aigc.repository.JsonFileStorageSupport;
import com.example.aigc.service.LocalAssetFileService;
import com.example.aigc.service.ProviderHttpGateway;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptRevision;
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
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonFileStorageSupport storageSupport;

    @MockitoSpyBean
    private LocalAssetFileService localAssetFileService;

    @MockitoSpyBean
    private ProviderHttpGateway providerHttpGateway;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("aigc.storage.data-dir", () -> DATA_DIR.toString());
        registry.add("aigc.pipeline.video.max-parallel", () -> 2);
        registry.add("aigc.pipeline.video.poll-interval-ms", () -> 10L);
        registry.add("aigc.pipeline.video.max-retries", () -> 1);
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

        JsonNode refined = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/refine", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refined.path("projectId").asText()).isEqualTo(projectId);
        assertThat(refined.path("refinedMarkdown").asText()).contains("## 故事摘要");
        assertThat(refined.path("structuredScript").path("segments").isArray()).isTrue();
        assertThat(refined.path("structuredScript").path("segments").size()).isGreaterThan(0);

        JsonNode refinedWithBrief = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/refine-with-brief", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("briefPrompt", "让节奏更紧凑，并突出阿满在清晨前的紧张与期待。"))))
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

        JsonNode updatedScript = readSuccessData(mockMvc.perform(put("/api/v1/script-projects/{projectId}/script", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateScriptRequest)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(updatedScript.path("structuredScript").path("characters").size()).isEqualTo(2);
        assertThat(updatedScript.path("structuredScript").path("props").size()).isEqualTo(2);

        JsonNode extractedCharacters = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/assets/extract/characters", projectId))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedBackgrounds = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/assets/extract/backgrounds", projectId))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode extractedProps = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/assets/extract/props", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(extractedCharacters.size()).isEqualTo(2);
        assertThat(extractedBackgrounds.size()).isEqualTo(1);
        assertThat(extractedProps.size()).isEqualTo(2);

        JsonNode assets = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}/assets", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(assets.size()).isEqualTo(5);

        String firstCharacterAssetId = extractedCharacters.get(0).path("assetId").asText();
        Map<String, Object> updateAssetRequest = new LinkedHashMap<>();
        updateAssetRequest.put("name", "阿满（定稿）");
        updateAssetRequest.put("description", "主角视觉设定定稿，保留画箱与清晨薄雾氛围。");
        updateAssetRequest.put("tags", List.of("角色", "主视觉", "定稿"));
        updateAssetRequest.put("promptDraft", "电影感写实，阿满背着画箱走过古镇清晨街口。");
        JsonNode updatedAsset = readSuccessData(mockMvc.perform(put("/api/v1/script-projects/{projectId}/assets/{assetId}", projectId, firstCharacterAssetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateAssetRequest)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(updatedAsset.path("name").asText()).isEqualTo("阿满（定稿）");
        assertThat(updatedAsset.path("tags").size()).isEqualTo(3);

        JsonNode shots = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/shots/split", projectId))
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

            JsonNode generatedKeyframes = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate", projectId, assetId))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(generatedKeyframes.size()).isEqualTo(2);

            String confirmedKeyframeId = generatedKeyframes.get(0).path("keyframeId").asText();
            JsonNode confirmedKeyframe = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/keyframes/{keyframeId}/confirm", projectId, confirmedKeyframeId))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(confirmedKeyframe.path("selected").asBoolean()).isTrue();
            if (selectedImageFileId == null) {
                selectedImageFileId = confirmedKeyframe.path("imageFileId").asText();
            }

            if (!regeneratedOnce) {
                JsonNode regenerated = readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/keyframes/{keyframeId}/regenerate", projectId, confirmedKeyframeId))
                        .andExpect(status().isOk())
                        .andReturn());
                assertThat(regenerated.size()).isEqualTo(2);
                regeneratedOnce = true;
            }
        }

        JsonNode keyframes = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}/keyframes", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(keyframes.size()).isEqualTo(8);
        assertThat(countSelected(keyframes)).isEqualTo(requiredAssetIds.size());
        assertThat(selectedAssetIds(keyframes)).isEqualTo(requiredAssetIds);
        assertThat(selectedImageFileId).isNotBlank();

        readSuccessData(mockMvc.perform(post("/api/v1/script-projects/{projectId}/video/generate", projectId))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode pipeline = waitForVideoCompletion(projectId);
        assertThat(pipeline.path("projectStatus").asText()).isEqualTo("COMPLETED");
        assertThat(pipeline.path("latestRun").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(pipeline.path("failedCount").asInt()).isZero();
        assertThat(pipeline.path("successCount").asInt()).isEqualTo(shots.size());

        JsonNode videoTasks = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}/video/tasks", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(videoTasks.size()).isEqualTo(1);
        assertThat(videoTasks.get(0).path("status").asText()).isEqualTo("SUCCESS");
        String videoFileId = videoTasks.get(0).path("resultVideoFileId").asText();
        assertThat(videoFileId).isNotBlank();

        MvcResult previewImage = mockMvc.perform(get("/api/v1/files/{fileId}", selectedImageFileId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(previewImage.getResponse().getContentType()).startsWith("image/svg+xml");
        assertThat(previewImage.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("<svg");

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

        Path projectRoot = localAssetFileService.resolveProjectRoot(projectId);
        Path projectJson = projectRoot.resolve("project.json");

        ScriptProjectAggregate aggregate = objectMapper.readValue(projectJson.toFile(), ScriptProjectAggregate.class);
        aggregate.revisions = new ArrayList<>();
        aggregate.revisions.add((ScriptRevision) null);
        storageSupport.writeValue(projectJson, aggregate);

        JsonNode revisions = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}/revisions", projectId))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(revisions.isArray()).isTrue();
        assertThat(revisions.size()).isEqualTo(0);
    }

    @Test
    void refineFailsWhenExplicitTextModelIsNotConfigured() throws Exception {
        String projectId = createTextProject("""
                夜色下，主角站在街角，准备拨通电话。
                """, "missing-text-model");

        JsonNode failedRefine = readResponseTree(mockMvc.perform(post("/api/v1/script-projects/{projectId}/refine", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(failedRefine.path("code").asInt()).isEqualTo(400);
        assertThat(failedRefine.path("message").asText()).contains("未命中已配置的文本模型");

        JsonNode detail = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(detail.path("project").path("status").asText()).isEqualTo("FAILED");

        JsonNode pipeline = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId))
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

        JsonNode uploadData = readSuccessData(mockMvc.perform(multipart("/api/v1/script-projects/upload")
                        .file(file)
                        .param("name", "DOCX Smoke Project")
                        .param("language", "zh-CN"))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode project = uploadData.path("project");
        String projectId = project.path("projectId").asText();
        assertThat(projectId).isNotBlank();
        assertThat(project.path("sourceType").asText()).isEqualTo("upload");
        assertThat(project.path("uploadedSourceFileId").asText()).isNotBlank();
        assertThat(uploadData.path("documents").size()).isEqualTo(2);

        JsonNode script = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}/script", projectId))
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

        mockMvc.perform(delete("/api/v1/script-projects/{projectId}", projectId))
                .andExpect(status().isOk());

        JsonNode detailAfterDelete = readResponseTree(mockMvc.perform(get("/api/v1/script-projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(detailAfterDelete.path("code").asInt()).isEqualTo(404);
        assertThat(detailAfterDelete.path("message").asText()).isEqualTo("剧本项目不存在");
        assertThat(Files.exists(projectRoot)).isFalse();

        Path indexFile = storageSupport.resolve("script-projects/index.json");
        String indexContent = Files.exists(indexFile) ? Files.readString(indexFile) : "";
        assertThat(indexContent).doesNotContain(projectId);
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

        JsonNode createData = readSuccessData(mockMvc.perform(post("/api/v1/script-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(createData.path("project").path("sourceType").asText()).isEqualTo("text");
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
        modelRequest.put("metadata", Map.of("capabilities", List.of("text")));
        readSuccessData(mockMvc.perform(post("/api/v1/models")
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
            latest = readSuccessData(mockMvc.perform(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId))
                    .andExpect(status().isOk())
                    .andReturn());
            if (latest.path("queuedCount").asInt() == 0 && latest.path("runningCount").asInt() == 0) {
                return latest;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("视频任务未在超时时间内完成: " + latest);
    }

    private JsonNode readSuccessData(MvcResult result) throws Exception {
        JsonNode root = readResponseTree(result);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        return root.path("data");
    }

    private JsonNode readResponseTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
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
