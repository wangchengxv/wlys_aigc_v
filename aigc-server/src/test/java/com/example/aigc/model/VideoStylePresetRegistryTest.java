package com.example.aigc.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoStylePresetRegistryTest {

    private final VideoStylePresetRegistry registry = new VideoStylePresetRegistry();

    @Test
    void normalizeStyleForWriteSupportsPresetTokenAndAlias() {
        assertThat(registry.normalizeStyleForWrite("preset:film-documentary")).isEqualTo("film-documentary");
        assertThat(registry.normalizeStyleForWrite("未来主义")).isEqualTo("futurism");
        assertThat(registry.normalizeStyleForWrite("misc-pop-art")).isEqualTo("pop-art");
    }

    @Test
    void resolveAnchorForReadFallsBackToRawStyleWhenUnknown() {
        assertThat(registry.resolveAnchorForRead("自定义诗意风格")).isEqualTo("自定义诗意风格");
    }

    @Test
    void resolveAnchorForReadResolvesKnownPresetAlias() {
        assertThat(registry.resolveAnchorForRead("preset:misc-nordic"))
                .contains("Nordic fresh style");
    }

    @Test
    void blankStyleUsesDefaultKey() {
        assertThat(registry.normalizeStyleForWrite("   ")).isEqualTo(VideoStylePresetRegistry.DEFAULT_STYLE_KEY);
    }

    @Test
    void normalizeVisualStyleForScriptProjectWritePreservesFreeFormAndMatchesWritePath() {
        assertThat(registry.normalizeVisualStyleForScriptProjectWrite("自定义诗意风格长段落"))
                .isEqualTo(registry.normalizeStyleForWrite("自定义诗意风格长段落"));
        assertThat(registry.normalizeVisualStyleForScriptProjectWrite("preset:film-cinematic"))
                .isEqualTo("live-action");
    }
}
