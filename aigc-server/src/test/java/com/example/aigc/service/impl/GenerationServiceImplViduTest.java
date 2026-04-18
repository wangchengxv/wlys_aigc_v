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

    private final GenerationServiceImpl service = new GenerationServiceImpl(
            Mockito.mock(GenerationTaskRepository.class),
            new AigcArkProperties(),
            Mockito.mock(ConnectionConfigRepository.class),
            Mockito.mock(ModelConfigRepository.class),
            Mockito.mock(ApiKeyCryptoService.class),
            new ProviderCatalog(),
            Mockito.mock(ProviderHttpGateway.class),
            new ModelCapabilityService(),
            Mockito.mock(RouterRoutingService.class),
            new VideoStylePresetRegistry(),
            Mockito.mock(LocalAssetFileService.class),
            Mockito.mock(ScriptProjectService.class)
    );

    @Test
    void validateAndNormalizeViduOptionsSupportsRecModeAndAudioRules() throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("duration", 8);
        payload.put("resolution", "720p");
        payload.put("audio", true);
        payload.put("audio_type", "ALL");
        payload.put("voice_id", "voice-a");
        payload.put("is_rec", true);
        payload.put("prompt", "test prompt");
        Map<String, Object> metadata = Map.of(
                "viduFamily", "q3",
                "viduDurations", List.of(4, 8),
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
        payload.put("duration", 6);
        Map<String, Object> metadata = Map.of(
                "viduFamily", "q2",
                "viduDurations", List.of(4, 8),
                "viduResolutions", List.of("360p", "540p", "720p"),
                "viduAudioSupported", true
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
                                "referenceImageUrl", "https://cdn.example.com/new.png",
                                "viduOptions", Map.of("duration", 8, "audio", true)
                        )
                ),
                "https://cdn.example.com/legacy.png",
                Map.of("duration", 4, "watermark", true)
        );

        Object normalized = invokePrivate(
                "normalizeAdvancedMedia",
                new Class<?>[]{GenerateRequest.class},
                request
        );

        Method refMethod = normalized.getClass().getDeclaredMethod("videoReferenceImageUrl");
        Method optionsMethod = normalized.getClass().getDeclaredMethod("videoViduOptions");
        assertThat(refMethod.invoke(normalized)).isEqualTo("https://cdn.example.com/new.png");
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
                "kling-v2",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "kling_multi_reference",
                                        "klingMultiReference", Map.of(
                                                "referenceImageUrls", List.of(
                                                        "https://cdn.example.com/ref-a.png",
                                                        "https://cdn.example.com/ref-b.png"
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
                "https://cdn.example.com/ref-a.png",
                "https://cdn.example.com/ref-b.png"
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
        Object resolvedModel = createResolvedModel("viduq2_reference2image", "vidu", Map.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) invokePrivate(
                "buildViduReference2ImagePayload",
                new Class<?>[]{resolvedModel.getClass(), String.class, List.class},
                resolvedModel,
                "让主体换一个场景",
                List.of("https://cdn.example.com/ref.png")
        );

        assertThat(payload)
                .containsEntry("model", "viduq2_reference2image")
                .containsEntry("prompt", "让主体换一个场景")
                .containsEntry("images", List.of("https://cdn.example.com/ref.png"));
    }

    @Test
    void buildKlingMultiReferencePayloadMapsReferenceImagesToSubjectImageList() throws Exception {
        Object resolvedModel = createResolvedModel("kling-v2", "onelinkai", Map.of());
        GenerateRequest request = new GenerateRequest(
                "融合两张参考图",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                2,
                "kling-v2",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "kling_multi_reference",
                                        "klingMultiReference", Map.of(
                                                "images", List.of(
                                                        "https://cdn.example.com/a.png",
                                                        "https://cdn.example.com/b.png"
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

        assertThat(payload).containsEntry("model_name", "kling-v2");
        assertThat(payload).containsEntry("n", 2);
        assertThat(payload.get("subject_image_list")).isEqualTo(List.of(
                Map.of("subject_image", "https://cdn.example.com/a.png"),
                Map.of("subject_image", "https://cdn.example.com/b.png")
        ));
    }

    @Test
    void buildKlingMultiReferencePayloadRejectsTooFewReferenceImages() throws Exception {
        Object resolvedModel = createResolvedModel("kling-v2", "onelinkai", Map.of());
        GenerateRequest request = new GenerateRequest(
                "只上传一张图",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "kling-v2",
                null,
                Map.of(
                        "image", Map.of(
                                "extra", Map.of(
                                        "capability", "kling_multi_reference",
                                        "klingMultiReference", Map.of(
                                                "images", List.of("https://cdn.example.com/a.png")
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
                        "url", "https://cdn.example.com/main.mp4",
                        "watermark_video_url", "https://cdn.example.com/watermark.mp4"
                ))
        );

        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) invokePrivate(
                "flattenViduVideoUrls",
                new Class<?>[]{Map.class},
                body
        );

        assertThat(urls).containsExactly(
                "https://cdn.example.com/main.mp4",
                "https://cdn.example.com/watermark.mp4"
        );
    }

    @Test
    void extractKlingTaskResultUrlsCollectsNestedImageUrls() throws Exception {
        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "task_result", Map.of(
                                "images", List.of(
                                        Map.of("url", "https://cdn.example.com/image-a.png"),
                                        Map.of("image_url", "https://cdn.example.com/image-b.png")
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
                "https://cdn.example.com/image-a.png",
                "https://cdn.example.com/image-b.png"
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
        Object resolvedModel = createResolvedModel("kling-v2", "onelinkai", Map.of());
        GenerateRequest request = new GenerateRequest(
                "把主体放到海边",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "kling-v2",
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
        Object resolvedModel = createResolvedModel("kling-v2", "onelinkai", Map.of());
        String imageRef = buildDataImageUrl(100, 50);
        GenerateRequest request = new GenerateRequest(
                "把主体放到海边",
                GenerateMode.image,
                "style",
                "1024x1024",
                "medium",
                1,
                "kling-v2",
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
