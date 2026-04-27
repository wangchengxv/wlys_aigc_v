package com.example.aigc.service.impl;

import com.example.aigc.config.AigcArkProperties;
import com.example.aigc.dto.GenerateRequest;
import com.example.aigc.enums.GenerateMode;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.VideoStylePresetRegistry;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.GenerationTaskRepository;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.service.ApiKeyCryptoService;
import com.example.aigc.service.GatewayKind;
import com.example.aigc.service.LocalAssetFileService;
import com.example.aigc.service.ModelCapabilityService;
import com.example.aigc.service.ProviderCatalog;
import com.example.aigc.service.ProviderCatalog.AuthMode;
import com.example.aigc.service.ProviderCatalog.ProviderDefinition;
import com.example.aigc.service.ProviderHttpGateway;
import com.example.aigc.service.RouterRoutingService;
import com.example.aigc.service.ScriptProjectService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationServiceImplViduTest {

    private final ProviderCatalog providerCatalog = new ProviderCatalog();
    private final ProviderHttpGateway providerHttpGateway = Mockito.mock(ProviderHttpGateway.class);
    private final GenerationServiceImpl service = new GenerationServiceImpl(
            Mockito.mock(GenerationTaskRepository.class),
            new AigcArkProperties(),
            Mockito.mock(ConnectionConfigRepository.class),
            Mockito.mock(ModelConfigRepository.class),
            Mockito.mock(ApiKeyCryptoService.class),
            providerCatalog,
            providerHttpGateway,
            new ModelCapabilityService(),
            Mockito.mock(RouterRoutingService.class),
            new VideoStylePresetRegistry(),
            Mockito.mock(LocalAssetFileService.class),
            Mockito.mock(ScriptProjectService.class)
    );

    @Test
    void validateAndNormalizeViduOptionsSupportsRecModeAndAudioRules() throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("duration", 5);
        payload.put("resolution", "1080p");
        payload.put("audio", true);
        payload.put("audio_type", "ALL");
        payload.put("voice_id", "voice-a");
        payload.put("is_rec", true);
        payload.put("prompt", "test prompt");
        Map<String, Object> metadata = Map.of(
                "viduFamily", "q3",
                "viduDurations", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                "viduResolutions", List.of("540p", "720p", "1080p"),
                "viduAudioSupported", true
        );

        invokePrivate(
                "validateAndNormalizeViduOptions",
                new Class<?>[]{Map.class, Map.class, String.class},
                payload,
                metadata,
                "viduq3-pro"
        );

        assertThat(payload.get("audio_type")).isEqualTo("all");
        assertThat(payload).doesNotContainKey("prompt");
        assertThat(String.valueOf(payload.get("meta_data"))).contains("rec_mode_enabled");
    }

    @Test
    void validateAndNormalizeViduOptionsRejectsUnsupportedDuration() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("duration", 11);
        Map<String, Object> metadata = Map.of(
                "viduFamily", "q2",
                "viduDurations", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                "viduResolutions", List.of("540p", "720p", "1080p"),
                "viduAudioSupported", false
        );

        assertThatThrownBy(() -> invokePrivate(
                "validateAndNormalizeViduOptions",
                new Class<?>[]{Map.class, Map.class, String.class},
                payload,
                metadata,
                "viduq2"
        ))
                .hasRootCauseInstanceOf(BizException.class)
                .rootCause()
                .extracting("status")
                .isEqualTo(400);
    }

    @Test
    void validateAndNormalizeViduOptionsRejectsAudioForQ2() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("duration", 5);
        payload.put("audio", true);
        Map<String, Object> metadata = Map.of(
                "viduFamily", "q2",
                "viduDurations", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                "viduResolutions", List.of("540p", "720p", "1080p"),
                "viduAudioSupported", false
        );

        assertThatThrownBy(() -> invokePrivate(
                "validateAndNormalizeViduOptions",
                new Class<?>[]{Map.class, Map.class, String.class},
                payload,
                metadata,
                "viduq2"
        ))
                .hasRootCauseInstanceOf(BizException.class)
                .rootCause()
                .extracting("status")
                .isEqualTo(400);
    }

    @Test
    void validateAndNormalizeViduOptionsRejectsAudioForQ1() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("duration", 5);
        payload.put("audio", true);
        Map<String, Object> metadata = Map.of(
                "viduFamily", "q1",
                "viduDurations", List.of(5),
                "viduResolutions", List.of("1080p"),
                "viduAudioSupported", false
        );

        assertThatThrownBy(() -> invokePrivate(
                "validateAndNormalizeViduOptions",
                new Class<?>[]{Map.class, Map.class, String.class},
                payload,
                metadata,
                "viduq1"
        ))
                .hasRootCauseInstanceOf(BizException.class)
                .rootCause()
                .extracting("status")
                .isEqualTo(400);
    }

    @Test
    void validateAndNormalizeViduOptionsRejectsNonFiveDurationForQ1() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("duration", 4);
        Map<String, Object> metadata = Map.of(
                "viduFamily", "q1",
                "viduDurations", List.of(5),
                "viduResolutions", List.of("1080p"),
                "viduAudioSupported", false
        );

        assertThatThrownBy(() -> invokePrivate(
                "validateAndNormalizeViduOptions",
                new Class<?>[]{Map.class, Map.class, String.class},
                payload,
                metadata,
                "viduq1"
        ))
                .hasRootCauseInstanceOf(BizException.class)
                .rootCause()
                .extracting("status")
                .isEqualTo(400);
    }

    @Test
    void normalizeAdvancedMediaPrefersStructuredVideoFieldsAndKeepsLegacyFallback() throws Exception {
        GenerateRequest request = new GenerateRequest(
                "prompt",
                GenerateMode.video,
                "style",
                "1024x1024",
                "medium",
                1,
                null,
                "viduq3-pro",
                Map.of(
                        "video", Map.of(
                                "referenceImageUrl", "https://api.onelinkai.cloud/new.png",
                                "viduOptions", Map.of("duration", 8, "audio", true)
                        )
                ),
                "https://api.onelinkai.cloud/legacy.png",
                Map.of("duration", 4, "watermark", true)
        );

        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );

        Method refMethod = normalized.getClass().getDeclaredMethod("videoReferenceImageUrl");
        Method optionsMethod = normalized.getClass().getDeclaredMethod("videoViduOptions");
        assertThat(refMethod.invoke(normalized)).isEqualTo("https://api.onelinkai.cloud/new.png");
        @SuppressWarnings("unchecked")
        Map<String, Object> options = (Map<String, Object>) optionsMethod.invoke(normalized);
        assertThat(options)
                .containsEntry("duration", 8)
                .containsEntry("audio", true)
                .containsEntry("watermark", true);
    }

    @Test
    void normalizeAdvancedMediaExtractsStructuredImageCapability() throws Exception {
        GenerateRequest request = new GenerateRequest(
                "prompt",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "video-kling-v3",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "kling_multi_reference",
                                        "klingMultiReference", Map.of(
                                                "referenceImageUrls", List.of(
                                                        "https://api.onelinkai.cloud/ref-a.png",
                                                        "https://api.onelinkai.cloud/ref-b.png"
                                                )
                                        )
                                )
                        )
                ),
                null,
                null
        );

        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Method imageMethod = normalized.getClass().getDeclaredMethod("image");
        Object image = imageMethod.invoke(normalized);
        Method capabilityMethod = image.getClass().getDeclaredMethod("capability");
        Method refsMethod = image.getClass().getDeclaredMethod("referenceImages");

        assertThat(capabilityMethod.invoke(image)).isEqualTo("kling_multi_reference");
        assertThat(refsMethod.invoke(image)).isEqualTo(List.of(
                "https://api.onelinkai.cloud/ref-a.png",
                "https://api.onelinkai.cloud/ref-b.png"
        ));
    }

    @Test
    void buildKlingOutpaintPayloadConvertsPixelMarginsToRatios() throws Exception {
        String imageRef = buildDataImageUrl(100, 50);
        GenerateRequest request = new GenerateRequest(
                "向四周延展画面",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "kling-outpaint",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "outpaint",
                                        "outpaint", Map.of(
                                                "sourceImageUrl", imageRef,
                                                "top", 10,
                                                "right", 20,
                                                "bottom", 5,
                                                "left", 15
                                        )
                                )
                        )
                ),
                null,
                null
        );

        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Object image = normalized.getClass().getDeclaredMethod("image").invoke(normalized);
        Object payload = invokePrivate(
                "buildKlingOutpaintPayload",
                new Class<?>[]{String.class, int.class, image.getClass()},
                "向四周延展画面",
                1,
                image
        );

        assertThat(payload)
                .isInstanceOf(Map.class)
                .extracting("up_expansion_ratio", "down_expansion_ratio", "left_expansion_ratio", "right_expansion_ratio")
                .containsExactly(0.2d, 0.1d, 0.15d, 0.2d);
    }

    @Test
    void buildViduReference2ImagePayloadKeepsPromptAndImages() throws Exception {
        Object resolvedModel = createResolvedModel("image-vidu-q2", "vidu", Map.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) invokePrivate(
                "buildViduReference2ImagePayload",
                new Class<?>[]{resolvedModel.getClass(), String.class, List.class},
                resolvedModel,
                "让主体换一个场景",
                List.of("https://api.onelinkai.cloud/ref.png")
        );

        assertThat(payload)
                .containsEntry("model", "image-vidu-q2")
                .containsEntry("prompt", "让主体换一个场景")
                .containsEntry("images", List.of("https://api.onelinkai.cloud/ref.png"));
    }

    @Test
    void buildViduReference2ImagePayloadOmitsImagesWhenEmptyForQ2TextToImage() throws Exception {
        Object resolvedModel = createResolvedModel("image-viduq3-pro", "vidu", Map.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) invokePrivate(
                "buildViduReference2ImagePayload",
                new Class<?>[]{resolvedModel.getClass(), String.class, List.class},
                resolvedModel,
                "只用提示词生成图片",
                List.of()
        );

        assertThat(payload)
                .containsEntry("model", "image-viduq3-pro")
                .containsEntry("prompt", "只用提示词生成图片")
                .doesNotContainKey("images");
    }

    @Test
    void buildViduReference2ImagePayloadRejectsUnsupportedModel() throws Exception {
        Object resolvedModel = createResolvedModel("image-vidu-q2-fast", "vidu", Map.of());

        assertPrivateBizException(
                400,
                "Vidu reference2image 仅支持模型：image-vidu-q2、image-viduq3-pro",
                () -> invokePrivate(
                        "buildViduReference2ImagePayload",
                        new Class<?>[]{resolvedModel.getClass(), String.class, List.class},
                        resolvedModel,
                        "测试",
                        List.of()
                )
        );
    }

    @Test
    void generateImagesWithViduReference2ImageRejectsQ1WithoutReferences() throws Exception {
        Object resolvedModel = createResolvedModel("viduq1", "vidu", Map.of());
        Class<?> imageRequestClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$NormalizedImageRequest");
        var ctor = imageRequestClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object imageRequest = ctor.newInstance(
                "vidu_reference2image",
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertPrivateBizException(
                400,
                "Vidu Q1 reference2image 需要至少 1 张参考图",
                () -> invokePrivate(
                        "generateImagesWithViduReference2Image",
                        new Class<?>[]{String.class, int.class, resolvedModel.getClass(), imageRequestClass},
                        "测试",
                        1,
                        resolvedModel,
                        imageRequest
                )
        );
    }

    @Test
    void generateImagesWithViduReference2ImageRejectsMoreThanSevenReferences() throws Exception {
        Object resolvedModel = createResolvedModel("image-vidu-q2", "vidu", Map.of());
        Class<?> imageRequestClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$NormalizedImageRequest");
        var ctor = imageRequestClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object imageRequest = ctor.newInstance(
                "vidu_reference2image",
                null,
                List.of(
                        "https://api.onelinkai.cloud/1.png",
                        "https://api.onelinkai.cloud/2.png",
                        "https://api.onelinkai.cloud/3.png",
                        "https://api.onelinkai.cloud/4.png",
                        "https://api.onelinkai.cloud/5.png",
                        "https://api.onelinkai.cloud/6.png",
                        "https://api.onelinkai.cloud/7.png",
                        "https://api.onelinkai.cloud/8.png"
                ),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertPrivateBizException(
                400,
                "Vidu reference2image 最多支持 7 张参考图",
                () -> invokePrivate(
                        "generateImagesWithViduReference2Image",
                        new Class<?>[]{String.class, int.class, resolvedModel.getClass(), imageRequestClass},
                        "测试",
                        1,
                        resolvedModel,
                        imageRequest
                )
        );
    }

    @Test
    void callViduReference2ImageApiUsesOneLinkBaseAndPathForViduOnelinkProvider() throws Exception {
        ProviderDefinition viduOnelink = providerCatalog.require("vidu_onelink");
        Mockito.when(providerHttpGateway.postJson(
                        Mockito.eq("https://api.onelinkai.cloud"),
                        Mockito.eq("/vidu/ent/v2/reference2image"),
                        Mockito.eq(viduOnelink),
                        Mockito.eq("secret-key"),
                        Mockito.eq(Map.of()),
                        Mockito.anyMap(),
                        Mockito.any()
                ))
                .thenReturn(Map.of("images", List.of("https://img.example/a.png")));

        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) invokePrivate(
                "callViduReference2ImageApi",
                new Class<?>[]{ProviderDefinition.class, String.class, String.class, Map.class, Map.class},
                viduOnelink,
                "https://api.vidu.cn",
                "secret-key",
                Map.of(),
                Map.of("model", "image-vidu-q2")
        );

        assertThat(urls).containsExactly("https://img.example/a.png");
    }

    @Test
    void validateViduImageRefAcceptsWebpDataHeader() throws Exception {
        String pngPayload = buildDataImageUrl(256, 256).substring("data:image/png;base64,".length());
        invokePrivate(
                "validateViduImageRef",
                new Class<?>[]{String.class},
                "data:image/webp;base64," + pngPayload
        );
    }

    @Test
    void validateViduImageRefRejectsTooSmallDimensions() throws Exception {
        String tooSmall = buildDataImageUrl(64, 64);
        assertPrivateBizException(
                400,
                "Vidu 参考图尺寸不能小于 128x128",
                () -> invokePrivate(
                        "validateViduImageRef",
                        new Class<?>[]{String.class},
                        tooSmall
                )
        );
    }

    @Test
    void validateViduImageRefRejectsOutOfRangeAspectRatio() throws Exception {
        String tooWide = buildDataImageUrl(700, 150);
        assertPrivateBizException(
                400,
                "Vidu 参考图比例不受支持，请使用 1:4-4:1 之间的宽高比",
                () -> invokePrivate(
                        "validateViduImageRef",
                        new Class<?>[]{String.class},
                        tooWide
                )
        );
    }

    @Test
    void buildKlingMultiReferencePayloadMapsReferenceImagesToSubjectImageList() throws Exception {
        Object resolvedModel = createResolvedModel("video-kling-v3", "onelinkai", Map.of());
        GenerateRequest request = new GenerateRequest(
                "融合两张参考图",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                2,
                "video-kling-v3",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "kling_multi_reference",
                                        "klingMultiReference", Map.of(
                                                "images", List.of(
                                                        "https://api.onelinkai.cloud/a.png",
                                                        "https://api.onelinkai.cloud/b.png"
                                                )
                                        )
                                )
                        )
                ),
                null,
                null
        );
        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Object image = normalized.getClass().getDeclaredMethod("image").invoke(normalized);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) invokePrivate(
                "buildKlingMultiReferencePayload",
                new Class<?>[]{resolvedModel.getClass(), String.class, int.class, image.getClass()},
                resolvedModel,
                "融合两张参考图",
                2,
                image
        );

        assertThat(payload).containsEntry("model_name", "video-kling-v3");
        assertThat(payload).containsEntry("n", 2);
        assertThat(payload.get("subject_image_list")).isEqualTo(List.of(
                Map.of("subject_image", "https://api.onelinkai.cloud/a.png"),
                Map.of("subject_image", "https://api.onelinkai.cloud/b.png")
        ));
    }

    @Test
    void buildKlingMultiReferencePayloadRejectsTooFewReferenceImages() throws Exception {
        Object resolvedModel = createResolvedModel("video-kling-v3", "onelinkai", Map.of());
        GenerateRequest request = new GenerateRequest(
                "只上传一张图",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "video-kling-v3",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "kling_multi_reference",
                                        "klingMultiReference", Map.of(
                                                "images", List.of("https://api.onelinkai.cloud/a.png")
                                        )
                                )
                        )
                ),
                null,
                null
        );
        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Object image = normalized.getClass().getDeclaredMethod("image").invoke(normalized);

        assertPrivateBizException(
                400,
                "Kling 多图参考生图需要 2~4 张参考图",
                () -> invokePrivate(
                        "buildKlingMultiReferencePayload",
                        new Class<?>[]{resolvedModel.getClass(), String.class, int.class, image.getClass()},
                        resolvedModel,
                        "只上传一张图",
                        1,
                        image
                )
        );
    }

    @Test
    void flattenViduVideoUrlsReturnsPrimaryThenWatermarkWithoutDuplicates() throws Exception {
        Map<String, Object> body = Map.of(
                "creations", List.of(Map.of(
                        "url", "https://api.onelinkai.cloud/main.mp4",
                        "watermark_video_url", "https://api.onelinkai.cloud/watermark.mp4"
                ))
        );

        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) invokePrivate(
                "flattenViduVideoUrls",
                new Class<?>[]{Map.class},
                body
        );

        assertThat(urls).containsExactly(
                "https://api.onelinkai.cloud/main.mp4",
                "https://api.onelinkai.cloud/watermark.mp4"
        );
    }

    @Test
    void extractKlingTaskResultUrlsCollectsNestedImageUrls() throws Exception {
        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "task_result", Map.of(
                                "images", List.of(
                                        Map.of("url", "https://api.onelinkai.cloud/image-a.png"),
                                        Map.of("image_url", "https://api.onelinkai.cloud/image-b.png")
                                )
                        )
                )
        );

        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) invokePrivate(
                "extractKlingTaskResultUrls",
                new Class<?>[]{Map.class, String.class},
                body,
                "images"
        );

        assertThat(urls).containsExactly(
                "https://api.onelinkai.cloud/image-a.png",
                "https://api.onelinkai.cloud/image-b.png"
        );
    }

    @Test
    void generateVideosWithKlingConnectionRejectsMissingReferenceImageForI2vModel() throws Exception {
        Object resolvedModel = createResolvedModel("kling-v1", "kling", Map.of());

        assertPrivateBizException(
                400,
                "Kling 图生视频模型需要参考图：请填写 videoReferenceImageUrl，或切换为文生视频模型",
                () -> invokePrivate(
                        "generateVideosWithKlingConnection",
                        new Class<?>[]{String.class, int.class, resolvedModel.getClass(), String.class},
                        "让角色向前走并回头",
                        1,
                        resolvedModel,
                        ""
                )
        );
    }

    @Test
    void generateVideosWithOneLinkConnectionRoutesDoubaoModelToVolcTaskApi() throws Exception {
        Object resolvedModel = createResolvedModel("doubao-seedance-2.0", "onelinkai", Map.of());

        Mockito.when(providerHttpGateway.postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of("task_id", "task-doubao-1"));
        Mockito.when(providerHttpGateway.getJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks/task-doubao-1"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of("data", Map.of("video_url", "https://api.onelinkai.cloud/doubao.mp4")));

        @SuppressWarnings("unchecked")
        List<String> videos = (List<String>) invokePrivate(
                "generateVideosWithOneLinkConnection",
                new Class<?>[]{String.class, int.class, resolvedModel.getClass(), String.class, Map.class, Map.class},
                "高速飞行镜头",
                1,
                resolvedModel,
                "https://api.onelinkai.cloud/ref.png",
                Map.of(),
                Map.of(
                        "referenceVideoUrls", List.of("https://api.onelinkai.cloud/ref.mp4"),
                        "referenceAudioUrls", List.of("https://api.onelinkai.cloud/ref.mp3"),
                        "generate_audio", true,
                        "ratio", "16:9",
                        "duration", 11,
                        "watermark", false
                )
        );

        assertThat(videos).containsExactly("https://api.onelinkai.cloud/doubao.mp4");
        Mockito.verify(providerHttpGateway).postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.argThat((Map<String, Object> payload) -> {
                    if (payload == null) {
                        return false;
                    }
                    if (!"doubao-seedance-2.0".equals(payload.get("model"))) {
                        return false;
                    }
                    Object content = payload.get("content");
                    if (!(content instanceof List<?> list) || list.size() != 4) {
                        return false;
                    }
                    if (!(list.get(0) instanceof Map<?, ?> textNode)
                            || !(list.get(1) instanceof Map<?, ?> imageNode)
                            || !(list.get(2) instanceof Map<?, ?> videoNode)
                            || !(list.get(3) instanceof Map<?, ?> audioNode)) {
                        return false;
                    }
                    if (!"text".equals(String.valueOf(textNode.get("type")))) {
                        return false;
                    }
                    Object imageUrlNode = imageNode.get("image_url");
                    if (!(imageUrlNode instanceof Map<?, ?> imageMap)) {
                        return false;
                    }
                    Object videoUrlNode = videoNode.get("video_url");
                    if (!(videoUrlNode instanceof Map<?, ?> videoMap)) {
                        return false;
                    }
                    Object audioUrlNode = audioNode.get("audio_url");
                    if (!(audioUrlNode instanceof Map<?, ?> audioMap)) {
                        return false;
                    }
                    return "image_url".equals(String.valueOf(imageNode.get("type")))
                            && "https://api.onelinkai.cloud/ref.png".equals(String.valueOf(imageMap.get("url")))
                            && "video_url".equals(String.valueOf(videoNode.get("type")))
                            && "reference_video".equals(String.valueOf(videoNode.get("role")))
                            && "https://api.onelinkai.cloud/ref.mp4".equals(String.valueOf(videoMap.get("url")))
                            && "audio_url".equals(String.valueOf(audioNode.get("type")))
                            && "reference_audio".equals(String.valueOf(audioNode.get("role")))
                            && "https://api.onelinkai.cloud/ref.mp3".equals(String.valueOf(audioMap.get("url")))
                            && Integer.valueOf(11).equals(payload.get("duration"))
                            && Boolean.FALSE.equals(payload.get("watermark"))
                            && Boolean.TRUE.equals(payload.get("generate_audio"))
                            && "16:9".equals(String.valueOf(payload.get("ratio")));
                }),
                Mockito.any()
        );
    }

    @Test
    void generateVideosWithOneLinkConnectionSupportsArkStyleIdAndContentVideoUrl() throws Exception {
        Object resolvedModel = createResolvedModel("doubao-seedance-2.0", "onelinkai", Map.of());

        Mockito.when(providerHttpGateway.postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of("id", "cgt-20260414164332-abc"));
        Mockito.when(providerHttpGateway.getJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks/cgt-20260414164332-abc"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of(
                "id", "cgt-20260414164332-abc",
                "status", "succeeded",
                "content", Map.of("videoUrl", "https://ark-acg-cn-beijing.tos-cn-beijing.volces.com/out.mp4")
        ));

        @SuppressWarnings("unchecked")
        List<String> videos = (List<String>) invokePrivate(
                "generateVideosWithOneLinkConnection",
                new Class<?>[]{String.class, int.class, resolvedModel.getClass(), String.class, Map.class, Map.class},
                "高速飞行镜头",
                1,
                resolvedModel,
                "https://api.onelinkai.cloud/ref.png",
                Map.of(),
                Map.of()
        );

        assertThat(videos).containsExactly("https://ark-acg-cn-beijing.tos-cn-beijing.volces.com/out.mp4");
    }

    @Test
    void generateVideosWithOneLinkConnectionNormalizesWrappedVideoUrlFromContentVideoUrl() throws Exception {
        Object resolvedModel = createResolvedModel("doubao-seedance-2.0", "onelinkai", Map.of());

        Mockito.when(providerHttpGateway.postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of("id", "cgt-20260414164332-wrap"));
        Mockito.when(providerHttpGateway.getJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks/cgt-20260414164332-wrap"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of(
                "status", "succeeded",
                "content", Map.of("videoUrl", " `https://ark-acg-cn-beijing.tos-cn-beijing.volces.com/wrapped.mp4` ")
        ));

        @SuppressWarnings("unchecked")
        List<String> videos = (List<String>) invokePrivate(
                "generateVideosWithOneLinkConnection",
                new Class<?>[]{String.class, int.class, resolvedModel.getClass(), String.class, Map.class, Map.class},
                "高速飞行镜头",
                1,
                resolvedModel,
                "https://api.onelinkai.cloud/ref.png",
                Map.of(),
                Map.of()
        );

        assertThat(videos).containsExactly("https://ark-acg-cn-beijing.tos-cn-beijing.volces.com/wrapped.mp4");
    }

    @Test
    void generateVideosWithOneLinkConnectionFailsFastWhenCodeIndicatesError() throws Exception {
        Object resolvedModel = createResolvedModel("doubao-seedance-2.0", "onelinkai", Map.of());

        Mockito.when(providerHttpGateway.postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of("id", "cgt-20260414164332-fail"));
        Mockito.when(providerHttpGateway.getJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks/cgt-20260414164332-fail"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.any()
        )).thenReturn(Map.of(
                "code", "AuthenticationError",
                "message", "the API key or AK/SK in the request is missing or invalid"
        ));

        assertPrivateBizException(
                502,
                "the API key or AK/SK in the request is missing or invalid",
                () -> invokePrivate(
                        "generateVideosWithOneLinkConnection",
                        new Class<?>[]{String.class, int.class, resolvedModel.getClass(), String.class, Map.class, Map.class},
                        "高速飞行镜头",
                        1,
                        resolvedModel,
                        "https://api.onelinkai.cloud/ref.png",
                        Map.of(),
                        Map.of()
                )
        );
        Mockito.verify(providerHttpGateway, Mockito.times(1)).getJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/contents/generations/tasks/cgt-20260414164332-fail"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.anyMap(),
                Mockito.any()
        );
    }

    @Test
    void generateVideosWithOneLinkConnectionRejectsMissingReferenceImageForDoubaoModel() throws Exception {
        Object resolvedModel = createResolvedModel("doubao-seedance-1.5-pro", "onelinkai", Map.of());

        assertPrivateBizException(
                400,
                "OneLink 豆包视频模型需要参考图：请在请求中填写 videoReferenceImageUrl（可访问的 http(s) 图片地址）",
                () -> invokePrivate(
                        "generateVideosWithOneLinkConnection",
                        new Class<?>[]{String.class, int.class, resolvedModel.getClass(), String.class, Map.class, Map.class},
                        "高速飞行镜头",
                        1,
                        resolvedModel,
                        "",
                        Map.of(),
                        Map.of()
                )
        );
    }

    @Test
    void generateImagesWithConfiguredModelRoutesKlingImageProviderThroughKlingImageApi() throws Exception {
        Object resolvedModel = createResolvedModel("image-kling-v3", "kling", Map.of());
        Class<?> resolvedClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$ResolvedModel");
        Class<?> normalizedMediaClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$NormalizedAdvancedMedia");

        assertPrivateBizException(
                502,
                "Kling 图片生成未返回 task_id",
                () -> invokePrivate(
                        "generateImagesWithConfiguredModel",
                        new Class<?>[]{String.class, int.class, resolvedClass, normalizedMediaClass},
                        "生成一张海报风格图片",
                        1,
                        resolvedModel,
                        null
                )
        );
    }

    @Test
    void generateImagesWithConfiguredModelRoutesOneLinkSeedreamToVolcImageApi() throws Exception {
        Object resolvedModel = createResolvedModel("doubao-seedream-4.0", "onelinkai", Map.of());
        Class<?> resolvedClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$ResolvedModel");
        Class<?> normalizedMediaClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$NormalizedAdvancedMedia");

        Mockito.when(providerHttpGateway.postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/images/generations"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.eq(Map.of()),
                Mockito.argThat((Map<String, Object> payload) ->
                        payload != null
                                && "doubao-seedream-4.0".equals(payload.get("model"))
                                && "赛博朋克城市夜景".equals(payload.get("prompt"))
                                && "disabled".equals(payload.get("sequential_image_generation"))
                                && "url".equals(payload.get("response_format"))
                                && "2K".equals(payload.get("size"))
                                && Boolean.FALSE.equals(payload.get("stream"))
                                && Boolean.TRUE.equals(payload.get("watermark"))
                ),
                Mockito.any()
        )).thenReturn(Map.of("data", List.of(Map.of("url", "https://api.onelinkai.cloud/seedream.png"))));

        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) invokePrivate(
                "generateImagesWithConfiguredModel",
                new Class<?>[]{String.class, int.class, resolvedClass, normalizedMediaClass},
                "赛博朋克城市夜景",
                1,
                resolvedModel,
                null
        );

        assertThat(images).containsExactly("https://api.onelinkai.cloud/seedream.png");
        Mockito.verify(providerHttpGateway, Mockito.times(1)).postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/images/generations"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.eq(Map.of()),
                Mockito.anyMap(),
                Mockito.any()
        );
    }

    @Test
    void generateImagesWithConfiguredModelKeepsNonSeedreamOneLinkOnDefaultPath() throws Exception {
        Object resolvedModel = createResolvedModel("wanx-v1", "onelinkai", Map.of());
        Class<?> resolvedClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$ResolvedModel");
        Class<?> normalizedMediaClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$NormalizedAdvancedMedia");

        assertPrivateBizException(
                400,
                "当前图片模型对应连接未配置图片生成接口",
                () -> invokePrivate(
                        "generateImagesWithConfiguredModel",
                        new Class<?>[]{String.class, int.class, resolvedClass, normalizedMediaClass},
                        "普通海报",
                        1,
                        resolvedModel,
                        null
                )
        );

        Mockito.verify(providerHttpGateway, Mockito.never()).postJson(
                Mockito.eq("https://example.com"),
                Mockito.eq("/volc/api/v3/images/generations"),
                Mockito.any(),
                Mockito.eq("secret"),
                Mockito.eq(Map.of()),
                Mockito.anyMap(),
                Mockito.any()
        );
    }

    @Test
    void buildKlingOutpaintPayloadRejectsMissingSourceImage() throws Exception {
        GenerateRequest request = new GenerateRequest(
                "向外扩图",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "kling-outpaint",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "outpaint",
                                        "outpaint", Map.of(
                                                "top", 10,
                                                "right", 0,
                                                "bottom", 0,
                                                "left", 0
                                        )
                                )
                        )
                ),
                null,
                null
        );
        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Object image = normalized.getClass().getDeclaredMethod("image").invoke(normalized);

        assertPrivateBizException(
                400,
                "扩图需要提供原图",
                () -> invokePrivate(
                        "buildKlingOutpaintPayload",
                        new Class<?>[]{String.class, int.class, image.getClass()},
                        "向外扩图",
                        1,
                        image
                )
        );
    }

    @Test
    void buildKlingOutpaintPayloadRejectsZeroExpandMargins() throws Exception {
        String imageRef = buildDataImageUrl(100, 50);
        GenerateRequest request = new GenerateRequest(
                "向外扩图",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "kling-outpaint",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "outpaint",
                                        "outpaint", Map.of(
                                                "sourceImageUrl", imageRef,
                                                "top", 0,
                                                "right", 0,
                                                "bottom", 0,
                                                "left", 0
                                        )
                                )
                        )
                ),
                null,
                null
        );
        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Object image = normalized.getClass().getDeclaredMethod("image").invoke(normalized);

        assertPrivateBizException(
                400,
                "扩图至少需要一个大于 0 的扩边值",
                () -> invokePrivate(
                        "buildKlingOutpaintPayload",
                        new Class<?>[]{String.class, int.class, image.getClass()},
                        "向外扩图",
                        1,
                        image
                )
        );
    }

    @Test
    void buildKlingOmniPayloadRejectsMissingSourceImage() throws Exception {
        Object resolvedModel = createResolvedModel("video-kling-v3", "onelinkai", Map.of());
        GenerateRequest request = new GenerateRequest(
                "把主体放到海边",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "video-kling-v3",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "omni",
                                        "omni", Map.of(
                                                "subjectPrompt", "一只戴墨镜的猫"
                                        )
                                )
                        )
                ),
                null,
                null
        );
        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Object image = normalized.getClass().getDeclaredMethod("image").invoke(normalized);

        assertPrivateBizException(
                400,
                "Omni 需要提供输入图",
                () -> invokePrivate(
                        "buildKlingOmniPayload",
                        new Class<?>[]{resolvedModel.getClass(), String.class, int.class, image.getClass()},
                        resolvedModel,
                        "把主体放到海边",
                        1,
                        image
                )
        );
    }

    @Test
    void buildKlingOmniPayloadRejectsMissingSubjectPrompt() throws Exception {
        Object resolvedModel = createResolvedModel("video-kling-v3", "onelinkai", Map.of());
        String imageRef = buildDataImageUrl(100, 50);
        GenerateRequest request = new GenerateRequest(
                "把主体放到海边",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "video-kling-v3",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "omni",
                                        "omni", Map.of(
                                                "sourceImageUrl", imageRef
                                        )
                                )
                        )
                ),
                null,
                null
        );
        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );
        Object image = normalized.getClass().getDeclaredMethod("image").invoke(normalized);

        assertPrivateBizException(
                400,
                "Omni 需要主体描述",
                () -> invokePrivate(
                        "buildKlingOmniPayload",
                        new Class<?>[]{resolvedModel.getClass(), String.class, int.class, image.getClass()},
                        resolvedModel,
                        "把主体放到海边",
                        1,
                        image
                )
        );
    }

    @Test
    void parseArkTaskStatusSupportsNestedTaskStatusField() throws Exception {
        Map<String, Object> body = Map.of(
                "data", Map.of("task_status", "submitted")
        );

        String status = (String) invokePrivate(
                "parseArkTaskStatus",
                new Class<?>[]{Map.class},
                body
        );

        assertThat(status).isEqualTo("submitted");
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = GenerationServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }

    private Object createResolvedModel(String modelName, String providerKey, Map<String, Object> metadata) throws Exception {
        ProviderDefinition provider = new ProviderDefinition(
                providerKey,
                providerKey,
                "https://example.com",
                providerKey,
                null,
                null,
                AuthMode.BEARER,
                false,
                null,
                "/submit",
                "/result/{taskId}",
                GatewayKind.OPENAI_COMPAT,
                List.of(modelName),
                null,
                false
        );

        com.example.aigc.model.ModelConfig model = new com.example.aigc.model.ModelConfig();
        model.setModelName(modelName);
        model.setMetadata(metadata);
        model.setProvider(providerKey);

        com.example.aigc.model.ConnectionConfig connection = new com.example.aigc.model.ConnectionConfig();
        connection.setBaseUrl("https://example.com");

        Class<?> resolvedClass = Class.forName("com.example.aigc.service.impl.GenerationServiceImpl$ResolvedModel");
        var constructor = resolvedClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object resolved = constructor.newInstance(
                model,
                connection,
                provider,
                "secret",
                metadata,
                "USER_CONFIGURED",
                "modelName",
                null
        );
        return resolved;
    }

    private void assertPrivateBizException(int status, String message, ThrowingCall call) {
        assertThatThrownBy(call::invoke)
                .isInstanceOf(InvocationTargetException.class)
                .hasRootCauseInstanceOf(BizException.class)
                .rootCause()
                .extracting("status", "message")
                .containsExactly(status, message);
    }

    private String buildDataImageUrl(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void invoke() throws Exception;
    }
}
