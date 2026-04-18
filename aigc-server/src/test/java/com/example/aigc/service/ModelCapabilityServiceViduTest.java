package com.example.aigc.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCapabilityServiceViduTest {

    private final ModelCapabilityService service = new ModelCapabilityService();

    @Test
    void resolveCapabilitiesTreatsViduModelAsVideo() {
        List<String> capabilities = service.resolveCapabilities(Map.of(), "onelinkai", "viduq3-pro");
        assertThat(capabilities).contains("video");
    }

    @Test
    void normalizeModelMetadataInjectsViduFamilyAndConstraintMatrix() {
        Map<String, Object> metadata = service.normalizeModelMetadata(Map.of(), "vidu", "viduq3-pro");

        assertThat(metadata.get("viduFamily")).isEqualTo("q3");
        assertThat(metadata.get("viduAudioSupported")).isEqualTo(true);
        assertThat(metadata.get("viduDurations")).isEqualTo(List.of(4, 8));
        assertThat(metadata.get("viduResolutions")).isEqualTo(List.of("540p", "720p", "1080p"));
        assertThat(metadata.get("capabilities")).isEqualTo(List.of("video"));
    }
}
