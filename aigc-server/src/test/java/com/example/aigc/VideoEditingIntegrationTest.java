package com.example.aigc;

import com.example.aigc.dto.ScriptProjectCreateRequest;
import com.example.aigc.entity.FinalCompositionInputSegment;
import com.example.aigc.entity.FinalCompositionTask;
import com.example.aigc.entity.LipSyncTask;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.enums.FinalCompositionTaskStatus;
import com.example.aigc.enums.LipSyncTaskStatus;
import com.example.aigc.enums.SegmentTaskStatus;
import com.example.aigc.enums.UserRole;
import com.example.aigc.service.LocalAssetFileService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.ScriptProjectService;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class VideoEditingIntegrationTest {

    private static final Path DATA_DIR = createDataDir();
    private static final String TEST_ACCESS_TOKEN = "dev-local-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptProjectService scriptProjectService;

    @MockitoSpyBean
    private LocalAssetFileService localAssetFileService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("aigc.storage.data-dir", () -> DATA_DIR.toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-video-edit;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
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
    }

    @AfterAll
    static void tearDown() throws IOException {
        deleteRecursively(DATA_DIR);
    }

    @Test
    void videoEditingWorkflowSupportsDraftSaveRenderPublishAndExportPriority() throws Exception {
        String projectId = createProject();
        seedVideoEditingSources(projectId);

        JsonNode draft = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}/video-editing/draft", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(draft.path("version").asInt()).isEqualTo(1);
        assertThat(draft.path("segments")).hasSize(2);
        assertThat(draft.path("segments").get(0).path("sourceType").asText()).isEqualTo("LIP_SYNC");
        assertThat(draft.path("segments").get(1).path("sourceType").asText()).isEqualTo("VIDEO");
        assertThat(draft.path("hasPublishedResult").asBoolean()).isFalse();

        List<Map<String, Object>> invalidSegments = objectMapper.convertValue(
                draft.path("segments"),
                new TypeReference<>() {}
        );
        invalidSegments.get(0).put("trimOutMs", 9000);
        Map<String, Object> invalidRequest = new LinkedHashMap<>();
        invalidRequest.put("expectedVersion", 1);
        invalidRequest.put("segments", invalidSegments);
        JsonNode invalidSave = readResponseTree(mockMvc.perform(
                        withAuth(put("/api/v1/script-projects/{projectId}/video-editing/draft", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(invalidRequest))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(invalidSave.path("code").asInt()).isEqualTo(400);
        assertThat(invalidSave.path("message").asText()).contains("裁切区间超出来源时长");

        List<Map<String, Object>> editedSegments = objectMapper.convertValue(
                draft.path("segments"),
                new TypeReference<>() {}
        );
        Map<String, Object> shot2 = editedSegments.get(1);
        Map<String, Object> shot1 = editedSegments.get(0);
        shot2.put("trimInMs", 500);
        shot2.put("trimOutMs", 4500);
        shot1.put("sourceType", "VIDEO");
        shot1.put("trimInMs", 250);
        shot1.put("trimOutMs", 3000);
        Map<String, Object> saveRequest = new LinkedHashMap<>();
        saveRequest.put("expectedVersion", 1);
        saveRequest.put("segments", List.of(shot2, shot1));
        saveRequest.put("extensions", Map.of("note", "manual-edit"));

        JsonNode savedDraft = readSuccessData(mockMvc.perform(
                        withAuth(put("/api/v1/script-projects/{projectId}/video-editing/draft", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(saveRequest))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(savedDraft.path("version").asInt()).isEqualTo(2);
        assertThat(savedDraft.path("segments").get(0).path("shotId").asText()).isEqualTo("shot-2");
        assertThat(savedDraft.path("segments").get(1).path("sourceType").asText()).isEqualTo("VIDEO");
        assertThat(savedDraft.path("hasUnpublishedChanges").asBoolean()).isTrue();

        readSuccessData(mockMvc.perform(withAuth(post("/api/v1/script-projects/{projectId}/video-editing/render/preview", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode previewPipeline = waitForVideoEditCompletion(projectId);
        assertThat(previewPipeline.path("latestRun").path("pipelineType").asText()).isEqualTo("VIDEO_EDIT_PREVIEW");
        assertThat(previewPipeline.path("videoEditRenderSuccessCount").asInt()).isEqualTo(1);

        JsonNode previewTasks = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}/video-editing/render/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(previewTasks).hasSize(1);
        assertThat(previewTasks.get(0).path("taskType").asText()).isEqualTo("PREVIEW");
        assertThat(previewTasks.get(0).path("status").asText()).isEqualTo("SUCCESS");
        String previewTaskId = previewTasks.get(0).path("renderTaskId").asText();
        String previewFileId = previewTasks.get(0).path("resultVideoFileId").asText();
        assertThat(previewFileId).isNotBlank();

        JsonNode publishWithoutVersion = readResponseTree(mockMvc.perform(
                        withAuth(post("/api/v1/script-projects/{projectId}/video-editing/render/publish", projectId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of("renderTaskId", previewTaskId))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(publishWithoutVersion.path("code").asInt()).isEqualTo(400);
        assertThat(publishWithoutVersion.path("message").asText()).contains("必须指定草稿版本");

        JsonNode publishWithMismatchedVersion = readResponseTree(mockMvc.perform(
                        withAuth(post("/api/v1/script-projects/{projectId}/video-editing/render/publish", projectId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of(
                                        "draftVersion", 999,
                                        "renderTaskId", previewTaskId
                                ))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(publishWithMismatchedVersion.path("code").asInt()).isEqualTo(400);
        assertThat(publishWithMismatchedVersion.path("message").asText()).contains("指定草稿版本与预览结果不匹配");

        readSuccessData(mockMvc.perform(
                        withAuth(post("/api/v1/script-projects/{projectId}/video-editing/render/publish", projectId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(Map.of(
                                        "draftVersion", 2,
                                        "renderTaskId", previewTaskId
                                ))))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode publishPipeline = waitForVideoEditCompletion(projectId);
        assertThat(publishPipeline.path("latestRun").path("pipelineType").asText()).isEqualTo("VIDEO_EDIT_PUBLISH");
        assertThat(publishPipeline.path("videoEditHasPublishedResult").asBoolean()).isTrue();
        assertThat(publishPipeline.path("videoEditHasUnpublishedChanges").asBoolean()).isFalse();

        JsonNode publishedDraft = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}/video-editing/draft", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(publishedDraft.path("publishedVersion").asInt()).isEqualTo(2);
        assertThat(publishedDraft.path("hasPublishedResult").asBoolean()).isTrue();

        JsonNode renderTasks = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}/video-editing/render/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(renderTasks).hasSize(1);
        JsonNode publishTask = renderTasks.get(0);
        assertThat(publishTask.path("renderTaskId").asText()).isEqualTo(previewTaskId);
        assertThat(publishTask.path("taskType").asText()).isEqualTo("PREVIEW");
        assertThat(publishTask.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(publishTask.path("publishedAt").asText()).isNotBlank();
        String publishTaskId = publishTask.path("renderTaskId").asText();

        readSuccessData(mockMvc.perform(withAuth(post("/api/v1/script-projects/{projectId}/export-package/generate", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode exportPipeline = waitForExportPackageCompletion(projectId);
        assertThat(exportPipeline.path("exportPackageReady").asBoolean()).isTrue();

        JsonNode exportTasks = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}/export-package/tasks", projectId)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode exportTask = exportTasks.get(0);
        assertThat(exportTask.path("sourceVideoOriginType").asText()).isEqualTo("VIDEO_EDIT_PUBLISHED");
        assertThat(exportTask.path("sourceVideoEditRenderTaskId").asText()).isEqualTo(publishTaskId);
        assertThat(exportTask.path("sourceFinalCompositionTaskId").isNull()).isTrue();

        MvcResult exportArchive = mockMvc.perform(get("/api/v1/files/{fileId}/download", exportTask.path("resultArchiveFileId").asText()))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, byte[]> archiveEntries = unzipEntries(exportArchive.getResponse().getContentAsByteArray());
        JsonNode manifest = objectMapper.readTree(archiveEntries.get("manifest.json"));
        assertThat(manifest.path("sourceVideoOriginType").asText()).isEqualTo("VIDEO_EDIT_PUBLISHED");
        assertThat(manifest.path("sourceTaskId").asText()).isEqualTo(publishTaskId);
        assertThat(manifest.path("shots")).hasSize(2);
        assertThat(manifest.path("shots").get(0).path("trimInMs").asInt()).isEqualTo(500);
    }

    private String createProject() {
        ScriptProjectAggregate aggregate = scriptProjectService.create(
                new RequestUserContext("integration-test-user", "integration-test-user", UserRole.ADMIN, "platform", null, true),
                new ScriptProjectCreateRequest(
                        "视频剪辑测试项目",
                        "第一幕：镜头一。第二幕：镜头二。",
                        "电影感写实",
                        null,
                        "16:9",
                        10,
                        "zh-CN",
                        null,
                        null,
                        null,
                        null
                )
        );
        return aggregate.project.projectId;
    }

    private void seedVideoEditingSources(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);

        StoryboardShot shot1 = new StoryboardShot();
        shot1.projectId = projectId;
        shot1.shotId = "shot-1";
        shot1.sequenceNo = 1;
        shot1.title = "清晨街口";
        shot1.scriptText = "主角走过街口。";
        shot1.actionSummary = "角色出场";
        shot1.cameraMovement = "跟拍";
        shot1.targetDurationSec = 4;
        shot1.createdAt = Instant.now();
        shot1.updatedAt = shot1.createdAt;

        StoryboardShot shot2 = new StoryboardShot();
        shot2.projectId = projectId;
        shot2.shotId = "shot-2";
        shot2.sequenceNo = 2;
        shot2.title = "停下回望";
        shot2.scriptText = "主角停下并回望。";
        shot2.actionSummary = "情绪停顿";
        shot2.cameraMovement = "推近";
        shot2.targetDurationSec = 5;
        shot2.createdAt = Instant.now();
        shot2.updatedAt = shot2.createdAt;

        StoredFileRecord shot1Video = localAssetFileService.storeBytes(projectId, "video/shot-1/result.mp4", "video/mp4", "video-1".getBytes(StandardCharsets.UTF_8));
        StoredFileRecord shot2Video = localAssetFileService.storeBytes(projectId, "video/shot-2/result.mp4", "video/mp4", "video-2".getBytes(StandardCharsets.UTF_8));
        StoredFileRecord shot1Lip = localAssetFileService.storeBytes(projectId, "lip-sync/shot-1/result.mp4", "video/mp4", "lip-1".getBytes(StandardCharsets.UTF_8));
        StoredFileRecord finalVideo = localAssetFileService.storeBytes(projectId, "final-composition/seed/result.mp4", "video/mp4", "final-video".getBytes(StandardCharsets.UTF_8));

        scriptProjectService.upsertFile(aggregate, shot1Video);
        scriptProjectService.upsertFile(aggregate, shot2Video);
        scriptProjectService.upsertFile(aggregate, shot1Lip);
        scriptProjectService.upsertFile(aggregate, finalVideo);

        VideoSegmentTask shot1VideoTask = new VideoSegmentTask();
        shot1VideoTask.segmentTaskId = "seg-1";
        shot1VideoTask.projectId = projectId;
        shot1VideoTask.shotId = "shot-1";
        shot1VideoTask.resultVideoFileId = shot1Video.fileId;
        shot1VideoTask.status = SegmentTaskStatus.SUCCESS;
        shot1VideoTask.finishedAt = Instant.now();

        VideoSegmentTask shot2VideoTask = new VideoSegmentTask();
        shot2VideoTask.segmentTaskId = "seg-2";
        shot2VideoTask.projectId = projectId;
        shot2VideoTask.shotId = "shot-2";
        shot2VideoTask.resultVideoFileId = shot2Video.fileId;
        shot2VideoTask.status = SegmentTaskStatus.SUCCESS;
        shot2VideoTask.finishedAt = Instant.now();

        LipSyncTask shot1LipTask = new LipSyncTask();
        shot1LipTask.lipSyncTaskId = "lip-1";
        shot1LipTask.projectId = projectId;
        shot1LipTask.shotId = "shot-1";
        shot1LipTask.resultVideoFileId = shot1Lip.fileId;
        shot1LipTask.status = LipSyncTaskStatus.SUCCESS;
        shot1LipTask.finishedAt = Instant.now();

        FinalCompositionInputSegment fc1 = new FinalCompositionInputSegment();
        fc1.shotId = "shot-1";
        fc1.sequenceNo = 1;
        fc1.shotTitle = "清晨街口";
        fc1.sourceType = "VIDEO";
        fc1.sourceTaskId = "seg-1";
        fc1.sourceFileId = shot1Video.fileId;
        fc1.sourcePublicUrl = shot1Video.publicUrl;

        FinalCompositionInputSegment fc2 = new FinalCompositionInputSegment();
        fc2.shotId = "shot-2";
        fc2.sequenceNo = 2;
        fc2.shotTitle = "停下回望";
        fc2.sourceType = "VIDEO";
        fc2.sourceTaskId = "seg-2";
        fc2.sourceFileId = shot2Video.fileId;
        fc2.sourcePublicUrl = shot2Video.publicUrl;

        FinalCompositionTask finalCompositionTask = new FinalCompositionTask();
        finalCompositionTask.finalCompositionTaskId = "final-seed";
        finalCompositionTask.projectId = projectId;
        finalCompositionTask.inputSegments = List.of(fc1, fc2);
        finalCompositionTask.resultVideoFileId = finalVideo.fileId;
        finalCompositionTask.status = FinalCompositionTaskStatus.SUCCESS;
        finalCompositionTask.finishedAt = Instant.now();

        aggregate.shots = new ArrayList<>(List.of(shot1, shot2));
        aggregate.videoTasks = new ArrayList<>(List.of(shot1VideoTask, shot2VideoTask));
        aggregate.lipSyncTasks = new ArrayList<>(List.of(shot1LipTask));
        aggregate.finalCompositionTasks = new ArrayList<>(List.of(finalCompositionTask));
        scriptProjectService.save(aggregate);
    }

    private JsonNode waitForVideoEditCompletion(String projectId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            latest = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
                    .andExpect(status().isOk())
                    .andReturn());
            if (latest.path("videoEditRenderQueuedCount").asInt() == 0 && latest.path("videoEditRenderRunningCount").asInt() == 0) {
                return latest;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("视频剪辑渲染任务未在超时时间内完成: " + latest);
    }

    private JsonNode waitForExportPackageCompletion(String projectId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        JsonNode latest = null;
        while (Instant.now().isBefore(deadline)) {
            latest = readSuccessData(mockMvc.perform(withAuth(get("/api/v1/script-projects/{projectId}/pipeline-status", projectId)))
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

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T withAuth(T builder) {
        return builder
                .header("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
                .header("x-aigc-token", TEST_ACCESS_TOKEN)
                .header("x-user-id", "integration-test-user")
                .header("x-user-name", "integration-test-user");
    }

    private static Path createDataDir() {
        try {
            return Files.createTempDirectory("aigc-server-video-edit-it-");
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
