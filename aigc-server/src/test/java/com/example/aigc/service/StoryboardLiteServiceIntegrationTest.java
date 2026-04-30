package com.example.aigc.service;

import com.example.aigc.dto.GenerateResponseData;
import com.example.aigc.dto.StoryboardLiteDtos;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.enums.TaskStatus;
import com.example.aigc.repository.StoredFileRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StoryboardLiteServiceIntegrationTest {
    private static final Path DATA_DIR = createDataDir();

    @Autowired
    private StoryboardLiteService storyboardLiteService;

    @Autowired
    private StoredFileRecordRepository storedFileRecordRepository;

    @MockBean
    private GenerationService generationService;

    @MockBean
    private ImageGenerationCapabilityService imageGenerationCapabilityService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("aigc.storage.data-dir", () -> DATA_DIR.toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:aigc-storyboard-lite-it;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> false);
    }

    @Test
    void generateKeyframesPersistsBase64ImageAsStoredFile() {
        String base64Image = Base64.getEncoder().encodeToString("fake-png".getBytes());
        when(generationService.generate(any(), eq("u1"))).thenReturn(new GenerateResponseData(
                "task-img",
                TaskStatus.SUCCESS,
                List.of(),
                List.of(base64Image),
                List.of(),
                "2026-04-30T00:00:00",
                12L,
                "prompt",
                GenerateMode.image,
                "影视级真实",
                "image-model",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of()
        ));

        StoryboardLiteDtos.SessionData session = storyboardLiteService.createSession(
                "u1",
                new StoryboardLiteDtos.CreateSessionRequest(null, "Lite Test")
        );
        storyboardLiteService.saveScript(
                "u1",
                session.sessionId(),
                new StoryboardLiteDtos.SaveScriptRequest("主角在白色布景前转身，展示服装和轮廓。")
        );

        List<StoryboardLiteDtos.KeyframeData> keyframes = storyboardLiteService.generateKeyframes(
                "u1",
                session.sessionId(),
                new StoryboardLiteDtos.GenerateKeyframesRequest(null, null, null)
        );

        assertThat(keyframes).hasSize(1);
        StoryboardLiteDtos.KeyframeData keyframe = keyframes.get(0);
        assertThat(keyframe.imageFileId()).isNotBlank();
        assertThat(keyframe.imageUrl()).startsWith("/api/v1/files/");
        assertThat(keyframe.imageUrl()).doesNotContain(base64Image);
        assertThat(keyframe.modelName()).isEqualTo("image-model");

        StoredFileRecord stored = storedFileRecordRepository.findByFileId(keyframe.imageFileId());
        assertThat(stored).isNotNull();
        assertThat(stored.projectId).isEqualTo("_aigc_workspace");
    }

    @Test
    void generateVideoUsesStoredImageAsDataUrlWhenLiteKeyframeWasPersistedLocally() {
        String base64Image = Base64.getEncoder().encodeToString("fake-png".getBytes());
        when(generationService.generate(any(), eq("u1")))
                .thenReturn(new GenerateResponseData(
                        "task-img",
                        TaskStatus.SUCCESS,
                        List.of(),
                        List.of(base64Image),
                        List.of(),
                        "2026-04-30T00:00:00",
                        12L,
                        "prompt",
                        GenerateMode.image,
                        "影视级真实",
                        "image-model",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of()
                ))
                .thenReturn(new GenerateResponseData(
                        "task-video",
                        TaskStatus.SUCCESS,
                        List.of(),
                        List.of(),
                        List.of("https://cdn.example.com/video.mp4"),
                        "2026-04-30T00:00:02",
                        24L,
                        "video-prompt",
                        GenerateMode.video,
                        "影视级真实",
                        null,
                        "video-model",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of()
                ));

        StoryboardLiteDtos.SessionData session = storyboardLiteService.createSession(
                "u1",
                new StoryboardLiteDtos.CreateSessionRequest(null, "Lite Video Test")
        );
        storyboardLiteService.saveScript(
                "u1",
                session.sessionId(),
                new StoryboardLiteDtos.SaveScriptRequest("主角面对镜头停顿，然后向左转身。")
        );

        StoryboardLiteDtos.KeyframeData keyframe = storyboardLiteService.generateKeyframes(
                "u1",
                session.sessionId(),
                new StoryboardLiteDtos.GenerateKeyframesRequest("custom-image-model", null, null)
        ).get(0);

        StoryboardLiteDtos.VideoTaskData videoTask = storyboardLiteService.generateVideo(
                "u1",
                session.sessionId(),
                new StoryboardLiteDtos.GenerateVideoRequest(keyframe.keyframeId(), "custom-video-model", null, "请基于参考图生成5秒电影感镜头。", null)
        );

        ArgumentCaptor<com.example.aigc.dto.GenerateRequest> captor = ArgumentCaptor.forClass(com.example.aigc.dto.GenerateRequest.class);
        verify(generationService, times(2)).generate(captor.capture(), eq("u1"));
        List<com.example.aigc.dto.GenerateRequest> requests = captor.getAllValues();
        assertThat(requests.get(0).imageModel()).isEqualTo("custom-image-model");
        assertThat(requests.get(1).videoModel()).isEqualTo("custom-video-model");
        assertThat(requests.get(1).videoReferenceImageUrl()).startsWith("data:image/");
        assertThat(keyframe.modelName()).isEqualTo("image-model");
        assertThat(videoTask.modelName()).isEqualTo("video-model");
    }

    @Test
    void generateVideoAcceptsDirectReferenceImageWithoutSelectedKeyframe() {
        when(generationService.generate(any(), eq("u1"))).thenReturn(new GenerateResponseData(
                "task-video-direct",
                TaskStatus.SUCCESS,
                List.of(),
                List.of(),
                List.of("https://cdn.example.com/direct-video.mp4"),
                "2026-04-30T00:00:03",
                30L,
                "video-prompt",
                GenerateMode.video,
                "影视级真实",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of()
        ));

        StoryboardLiteDtos.SessionData session = storyboardLiteService.createSession(
                "u1",
                new StoryboardLiteDtos.CreateSessionRequest(null, "Lite Direct Ref Test")
        );
        String referenceImage = "data:image/png;base64," + Base64.getEncoder().encodeToString("direct-png".getBytes());

        StoryboardLiteDtos.VideoTaskData videoTask = storyboardLiteService.generateVideo(
                "u1",
                session.sessionId(),
                new StoryboardLiteDtos.GenerateVideoRequest(null, "custom-video-model", null, "请基于上传首帧生成视频。", referenceImage)
        );

        ArgumentCaptor<com.example.aigc.dto.GenerateRequest> captor = ArgumentCaptor.forClass(com.example.aigc.dto.GenerateRequest.class);
        verify(generationService).generate(captor.capture(), eq("u1"));
        assertThat(captor.getValue().videoModel()).isEqualTo("custom-video-model");
        assertThat(captor.getValue().videoReferenceImageUrl()).isEqualTo(referenceImage);
        assertThat(videoTask.keyframeId()).isNull();
        assertThat(videoTask.modelName()).isEqualTo("custom-video-model");
    }

    private static Path createDataDir() {
        try {
            return Files.createTempDirectory("aigc-storyboard-lite-it-");
        } catch (Exception ex) {
            throw new IllegalStateException("创建测试目录失败", ex);
        }
    }
}
