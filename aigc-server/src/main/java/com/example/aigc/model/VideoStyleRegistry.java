package com.example.aigc.model;

import com.example.aigc.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Legacy 14-key style registry (workspace / generate 场景历史实现). Prefer {@link VideoStylePresetRegistry}
 * for new code; this bean is retained for compatibility and is not used by script-project create flow.
 */
@Component
public class VideoStyleRegistry {

    private static final String DEFAULT_STYLE_KEY = "live-action";
    private static final String ENGLISH_NEGATIVE_PROMPT = "no clipping, no distortion, no extra limbs, no morphing faces, no motion blur artifacts, stabilized frame, natural fluid motion, no perspective errors, no duplicate subjects, no watermarks, no text overlays unless specified";

    private final Map<String, String> styleAnchors;
    private final Map<String, String> legacyToKey;

    public VideoStyleRegistry() {
        LinkedHashMap<String, String> anchors = new LinkedHashMap<>();
        anchors.put("live-action", "photorealistic live-action footage, cinematic film quality, real human actors with natural skin texture and subsurface scattering, professional cinematography with anamorphic lens characteristics, natural mixed lighting with motivated sources, 8K resolution, shallow depth of field with creamy bokeh, subtle film grain texture, cinematic color grade, anamorphic lens flare on backlit edges, three-point lighting setup, ultra-smooth camera motion, no CGI artifacts");
        anchors.put("anime", "Japanese anime style, cel-shaded flat coloring with precise shadow bands, vibrant saturated color palette, large expressive eyes with multi-layered iris highlights and specular dots, dynamic action poses with exaggerated motion blur streaks, clean sharp outlines with consistent line weight, flowing hair with defined highlight ribbons, Studio Ghibli / Makoto Shinkai production quality, hand-painted atmospheric sky backgrounds with soft cloud gradients, soft ambient fill light with dramatic colored rim light on character edges, sakura or particle effects for atmosphere");
        anchors.put("2d-animation", "classic 2D hand-drawn animation, Disney golden-age / Pixar 2D hybrid quality, smooth clean lines with consistent stroke weight, expressive character acting using squash-and-stretch and anticipation principles, painterly watercolor or gouache backgrounds with visible brushwork, soft gradient shading with warm color palette, round friendly proportions with appeal-driven design, fluid 24fps animation timing, secondary motion on hair and clothing");
        anchors.put("3d-animation", "high-quality 3D CGI animation, Pixar / DreamWorks production quality, stylized character proportions with appealing design, subsurface scattering on skin and organic materials, detailed physically-based rendering (PBR) textures, volumetric lighting with soft area light shadows, ambient occlusion in crevices, global illumination color bleed, smooth motion blur on fast movement, expressive facial rigging with blend shape detail, polished environment shading with depth haze");
        anchors.put("cyberpunk", "cyberpunk aesthetic, rain-soaked neon-lit urban night environment, reflective wet asphalt mirroring layered holographic signage, volumetric fog with neon color bleeding and light shaft penetration, high-tech low-life contrast, Blade Runner 2049 cinematic style, chromatic aberration on lens edges, cool blue-purple dominant palette with hot pink and cyan accent neons, gritty micro-detail textures on worn surfaces, lens flare from point light sources, holographic UI overlays with glitch artifacts, shallow depth of field");
        anchors.put("oil-painting", "classical oil painting style, visible directional impasto brushstrokes with palette knife texture, rich multi-layer glazing depth, warm chromatic undertones, museum-quality fine art composition following the golden ratio, Rembrandt three-quarter dramatic lighting, strong chiaroscuro contrast between illuminated and shadow zones, canvas weave texture visible throughout, painterly edge treatment with lost-and-found contours, Baroque color richness with deep shadow detail");
        anchors.put("ink-wash", "traditional Chinese ink-wash painting style, sumi-e technique, ink bleeding naturally into absorbent rice paper ground, rich tonal gradation from dense black to translucent grey washes, deliberate generous negative space for East Asian compositional breathing room, lost soft edges where ink diffuses into damp paper, monochromatic blue-grey atmospheric palette with occasional muted earth accent, brushwork texture visible at stroke edges, poetic misty depth layers, minimal color - ink tone variation carries all narrative weight");
        anchors.put("ancient-chinese", "classical Chinese aesthetic, Tang-Song dynasty visual language, traditional hanfu garments with layered silk fabric and wide sleeves, soft directional light filtered through rice-paper screens, symmetrical architectural framing with carved wooden lattice and stone lanterns, warm ivory and ink-green and ochre-red color palette, falling petals and flowing water as atmospheric elements, unhurried meditative pacing, no modern materials or objects, painterly soft-focus background treatment, shallow depth of field on fabric texture detail");
        anchors.put("cyber-dunhuang", "Cyber-Dunhuang fusion aesthetic, Tang Dynasty Apsara celestial figures reimagined in digital neon space, traditional Dunhuang fresco motifs rendered in holographic pixel fragments, deep violet and electric blue and burnished gold color palette, neon particle streams echoing ribbon dance movement, zero-gravity floating architecture referencing Buddhist cave-temple geometry, glitch artifacts on ancient pattern transitions, volumetric light shafts through digital void, high contrast between warm traditional pigment tones and cold neon technology");
        anchors.put("watercolor", "hand-painted watercolor illustration style, transparent pigment washes bleeding softly into damp paper, visible paper grain and watermark texture throughout, wet-on-wet background treatment with color blooming at edges, dry-brush detail strokes for foreground elements, Morandi-inspired desaturated pastel palette, natural color pooling and tide-mark edges where washes dried, no hard black outlines - edges defined only by tonal contrast, light and airy composition with generous white paper showing through");
        anchors.put("retro-film", "vintage analog film aesthetic, Kodak 5219 film stock simulation, visible silver-halide grain structure with natural random distribution, slightly desaturated color with warm orange-red midtone shift, blue-shifted shadows, gentle vignette darkening at frame edges, soft halation bloom around highlights and backlit edges, occasional subtle lens flare streaks, natural lens distortion with mild barrel effect at wide angle, no digital sharpening, no HDR processing, authentic analog imperfection throughout");
        anchors.put("gothic-dark", "dark Gothic aesthetic, medieval cathedral or Victorian interior setting, extreme high-contrast chiaroscuro with deep shadowless blacks, single warm amber candlelight as the only motivated light source, stone or aged wood textures with lichen and weathering detail, heavy velvet drapery with embroidered motifs, slow deliberate movement with oppressive atmospheric stillness, cool shadow tones against isolated warm light pools, no fill light - intentional large shadow areas, mysterious and airless mood");
        anchors.put("vaporwave", "vaporwave aesthetic, 1980s retro-futurist digital nostalgia, infinite purple-pink grid floor receding to horizon, magenta-to-violet gradient sky, floating Greco-Roman marble bust with intentional damage and museum patina, vintage beige CRT monitor screens flickering with pixelated waveforms, pixelated palm tree silhouettes in bright orange outline, high-saturation complementary color clash (deep purple, magenta, electric blue, coral-orange), VHS scan line overlay artifact, no anti-aliasing, deliberate lo-fi digital texture");
        anchors.put("pixel-art", "retro 8-bit pixel art style, chunky visible pixel blocks with no anti-aliasing, deliberately limited color palette per sprite, hard pixel-perfect edges on all elements, classic video game sprite proportions, scanline texture overlay for CRT monitor feel, deliberately low frame rate animation (12fps) for authentic retro movement, bright punchy saturated colors, blocky terrain and environment tiles, sprite-based character design with minimal pixel detail, chiptune aesthetic visual language");
        this.styleAnchors = Map.copyOf(anchors);

        LinkedHashMap<String, String> compatibility = new LinkedHashMap<>();
        compatibility.put("电影感写实", "live-action");
        compatibility.put("电影感", "live-action");
        compatibility.put("realistic", "live-action");
        compatibility.put("live action", "live-action");
        compatibility.put("liveaction", "live-action");
        compatibility.put("水墨国风", "ink-wash");
        compatibility.put("古风", "ancient-chinese");
        compatibility.put("古风雅韵", "ancient-chinese");
        compatibility.put("赛博敦煌", "cyber-dunhuang");
        compatibility.put("蒸汽波", "vaporwave");
        compatibility.put("像素风", "pixel-art");
        compatibility.put("油画", "oil-painting");
        this.legacyToKey = Map.copyOf(compatibility);
    }

    public String defaultKey() {
        return DEFAULT_STYLE_KEY;
    }

    public String englishNegativePrompt() {
        return ENGLISH_NEGATIVE_PROMPT;
    }

    public List<String> supportedKeys() {
        return styleAnchors.keySet().stream().toList();
    }

    public Set<String> supportedKeySet() {
        return styleAnchors.keySet();
    }

    public String supportedKeysText() {
        return supportedKeys().stream().collect(Collectors.joining(", "));
    }

    public String toCanonicalKey(String rawStyle) {
        if (rawStyle == null) {
            return DEFAULT_STYLE_KEY;
        }
        String normalized = rawStyle.trim();
        if (normalized.isBlank()) {
            return DEFAULT_STYLE_KEY;
        }
        if (styleAnchors.containsKey(normalized)) {
            return normalized;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (styleAnchors.containsKey(lower)) {
            return lower;
        }
        String mapped = legacyToKey.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        return null;
    }

    public String normalizeForInputOrThrow(String rawStyle, String fieldName) {
        String canonical = toCanonicalKey(rawStyle);
        if (canonical == null) {
            throw new BizException(400, fieldName + "不合法，仅支持以下风格key: " + supportedKeysText());
        }
        return canonical;
    }

    public String normalizeForStoredProject(String rawStyle) {
        String canonical = toCanonicalKey(rawStyle);
        return canonical == null ? DEFAULT_STYLE_KEY : canonical;
    }

    public String anchorByKey(String styleKey) {
        String key = normalizeForInputOrThrow(styleKey, "style");
        return styleAnchors.get(key);
    }

    public String visualStyleAnchorLine(String styleKey) {
        return "Visual Style Anchor: " + anchorByKey(styleKey);
    }
}
