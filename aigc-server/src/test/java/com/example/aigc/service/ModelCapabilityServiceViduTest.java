package com.example.aigc.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCapabilityServiceViduTest {

    private final ModelCapabilityService service = new ModelCapabilityService();

    @Test
    void resolveCapabilitiesTreatsViduQ3ModelAsVideo() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "onelinkai", "viduq3-pro");
        assertThat(capabilities).contains("video");
    }

    @Test
    void resolveCapabilitiesTreatsViduQ2FamilyAsImageAndVideo() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "vidu", "viduq2");
        assertThat(capabilities).contains("image", "video");
    }

    @Test
    void resolveCapabilitiesTreatsImageViduPrefixAsImageAndVideo() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "vidu_onelink", "image-vidu-q2-fast");
        assertThat(capabilities).contains("image", "video");
    }

    @Test
    void normalizeModelMetadataInjectsViduFamilyAndConstraintMatrix() {
        Map<String, Object> metadata = service.normalizeModelMetadata(Map.of(), "vidu", "viduq2");

        assertThat(metadata.get("viduFamily")).isEqualTo("q2");
        assertThat(metadata.get("viduAudioSupported")).isEqualTo(false);
        assertThat(metadata.get("viduDurations")).isEqualTo(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(metadata.get("viduResolutions")).isEqualTo(List.of("540p", "720p", "1080p"));
        assertThat(metadata.get("capabilities")).isEqualTo(List.of("image", "video"));
    }

    @Test
    void normalizeModelMetadataInjectsQ3MatrixWithAudioEnabled() {
        Map<String, Object> metadata = service.normalizeModelMetadata(Map.of(), "vidu", "video-viduq3-pro");

        assertThat(metadata.get("viduFamily")).isEqualTo("q3");
        assertThat(metadata.get("viduAudioSupported")).isEqualTo(true);
        assertThat(metadata.get("viduDurations")).isEqualTo(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        assertThat(metadata.get("viduResolutions")).isEqualTo(List.of("540p", "720p", "1080p"));
    }

    @Test
    void normalizeModelMetadataInjectsQ1FixedMatrixWithoutAudio() {
        Map<String, Object> metadata = service.normalizeModelMetadata(Map.of(), "vidu", "viduq1");

        assertThat(metadata.get("viduFamily")).isEqualTo("q1");
        assertThat(metadata.get("viduAudioSupported")).isEqualTo(false);
        assertThat(metadata.get("viduDurations")).isEqualTo(List.of(5));
        assertThat(metadata.get("viduResolutions")).isEqualTo(List.of("1080p"));
    }

    @Test
    void resolveCapabilitiesTreatsKlingProviderV1AsVideo() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "kling", "kling-v1");
        assertThat(capabilities).contains("video");
    }

    @Test
    void resolveCapabilitiesTreatsOneLinkKlingV21AsImage() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "onelinkai", "video-kling-v3");
        assertThat(capabilities).contains("image", "video");
    }

    @Test
    void resolveCapabilitiesTreatsKlingImageV3AsImageOnly() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "kling", "image-kling-v3");
        assertThat(capabilities).containsExactly("image");
    }

    @Test
    void resolveCapabilitiesTreatsKlingImageV3OmniAsImageOnly() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "kling", "image-kling-v3-omni");
        assertThat(capabilities).containsExactly("image");
    }

    @Test
    void resolveCapabilitiesTreatsImageViduQ3ProAsImageOnly() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "onelinkai", "image-viduq3-pro");
        assertThat(capabilities).containsExactly("image");
    }
}
