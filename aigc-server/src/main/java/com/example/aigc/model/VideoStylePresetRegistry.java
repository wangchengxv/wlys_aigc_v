package com.example.aigc.model;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class VideoStylePresetRegistry {

    public static final String DEFAULT_STYLE_KEY = "live-action";

    private static final String NEGATIVE_PROMPT_ENGLISH = "no clipping, no distortion, no extra limbs, no morphing faces, no motion blur artifacts, stabilized frame, natural fluid motion, no perspective errors, no duplicate subjects, no watermarks, no text overlays unless specified";

    private static final Map<String, String> STYLE_ANCHORS = buildStyleAnchors();

    private static final Map<String, String> STYLE_KEY_ALIASES = buildStyleKeyAliases();

    public List<String> supportedKeys() {
        return new ArrayList<>(STYLE_ANCHORS.keySet());
    }

    public boolean isSupportedKey(String styleKey) {
        return STYLE_ANCHORS.containsKey(normalize(styleKey));
    }

    public String requireStyleKey(String styleKey, String fieldName) {
        return normalizeStyleForWrite(styleKey);
    }

    public String normalizeStyleForWrite(String styleKey) {
        String normalized = normalizeAliasToken(styleKey);
        if (normalized.isBlank()) {
            return DEFAULT_STYLE_KEY;
        }
        if (STYLE_ANCHORS.containsKey(normalized)) {
            return normalized;
        }
        String mapped = STYLE_KEY_ALIASES.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        return styleKey == null ? DEFAULT_STYLE_KEY : styleKey.trim();
    }

    /**
     * Normalizes persisted {@code visualStyle} for script projects: resolves {@code preset:} tokens and
     * dictionary aliases to canonical keys when possible; otherwise keeps non-blank free-form text as-is.
     * Script workflows may store user-facing descriptions or long prompt text — this must not reject them.
     */
    public String normalizeVisualStyleForScriptProjectWrite(String visualStyle) {
        return normalizeStyleForWrite(visualStyle);
    }

    public String normalizeStyleKeyForRead(String rawStyleKey) {
        return normalizeStyleForWrite(rawStyleKey);
    }

    public String resolveAnchorByStyleKey(String styleKey) {
        String normalized = normalizeAliasToken(styleKey);
        if (normalized.isBlank()) {
            return STYLE_ANCHORS.get(DEFAULT_STYLE_KEY);
        }
        if (STYLE_ANCHORS.containsKey(normalized)) {
            return STYLE_ANCHORS.get(normalized);
        }
        String mapped = STYLE_KEY_ALIASES.get(normalized);
        if (mapped != null) {
            return STYLE_ANCHORS.get(mapped);
        }
        return styleKey.trim();
    }

    public String resolveAnchorForRead(String rawStyleKey) {
        return resolveAnchorByStyleKey(rawStyleKey);
    }

    public String videoNegativePromptEnglish() {
        return NEGATIVE_PROMPT_ENGLISH;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeAliasToken(String value) {
        String normalized = normalize(value);
        if (normalized.startsWith("preset:")) {
            return normalize(normalized.substring("preset:".length()));
        }
        return normalized;
    }

    private static Map<String, String> buildStyleAnchors() {
        Map<String, String> anchors = new LinkedHashMap<>();
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
        anchors.put("retro-hk", "retro Hong Kong cinema aesthetic, 1980s-1990s neon street mood, humid rainy-night atmosphere, red-orange and cyan-teal contrast palette, practical neon signage reflections, subtle analog film grain, medium-close character-driven framing, expressive motivated lighting, emotional urban nostalgia");
        anchors.put("retro-8mm", "8mm home-video nostalgia style, heavy film grain and gate weave, slight flicker and light leak artifacts, faded warm tones, handheld imperfect framing, low-frame-rate memory texture, candid documentary intimacy, family-archive mood, intentionally imperfect analog realism");
        anchors.put("gothic-dark", "dark Gothic aesthetic, medieval cathedral or Victorian interior setting, extreme high-contrast chiaroscuro with deep shadowless blacks, single warm amber candlelight as the only motivated light source, stone or aged wood textures with lichen and weathering detail, heavy velvet drapery with embroidered motifs, slow deliberate movement with oppressive atmospheric stillness, cool shadow tones against isolated warm light pools, no fill light - intentional large shadow areas, mysterious and airless mood");
        anchors.put("film-documentary", "documentary visual language, observational framing, natural available light, authentic real-world texture, restrained color grading, subtle handheld drift, truthful environmental details, non-staged candid moments, editorial realism, journalistic cinematic clarity");
        anchors.put("film-noir", "film noir style, high-contrast monochrome look, Venetian blind shadow patterns, low-key practical lighting, smoky atmosphere, suspenseful framing, dramatic silhouettes, moral ambiguity mood, hard-edge highlights with deep blacks, classic detective-era cinematic tension");
        anchors.put("film-commercial", "premium commercial advertising style, clean product-first composition, precision studio lighting, polished reflective surfaces, controlled highlight roll-off, smooth motion-control camera movement, crisp micro-detail, brand-safe color separation, high-end glossy finish");
        anchors.put("vaporwave", "vaporwave aesthetic, 1980s retro-futurist digital nostalgia, infinite purple-pink grid floor receding to horizon, magenta-to-violet gradient sky, floating Greco-Roman marble bust with intentional damage and museum patina, vintage beige CRT monitor screens flickering with pixelated waveforms, pixelated palm tree silhouettes in bright orange outline, high-saturation complementary color clash (deep purple, magenta, electric blue, coral-orange), VHS scan line overlay artifact, no anti-aliasing, deliberate lo-fi digital texture");
        anchors.put("pixel-art", "retro 8-bit pixel art style, chunky visible pixel blocks with no anti-aliasing, deliberately limited color palette per sprite, hard pixel-perfect edges on all elements, classic video game sprite proportions, scanline texture overlay for CRT monitor feel, deliberately low frame rate animation (12fps) for authentic retro movement, bright punchy saturated colors, blocky terrain and environment tiles, sprite-based character design with minimal pixel detail, chiptune aesthetic visual language");
        anchors.put("futurism", "minimalist futurism style, geometric architecture and clean line language, cool metallic white-silver palette with cyan accents, soft ambient volumetric illumination, high-tech materials with subtle reflections, sparse yet purposeful composition, elegant precision and forward-looking atmosphere");
        anchors.put("nordic", "Nordic fresh style, soft daylight and airy interior/exterior atmosphere, white-gray with pale blue-green palette, low-contrast clean textures, minimalist composition with generous negative space, cozy calm lifestyle tone, delicate natural materials and uncluttered modern simplicity");
        anchors.put("pop-art", "pop art style, bold graphic outlines, halftone dot textures, striking complementary color clashes, poster-like flat composition, comic-inspired visual rhythm, playful exaggerated shapes, high-energy contemporary street-art attitude");
        return anchors;
    }

    private static Map<String, String> buildStyleKeyAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();

        // Legacy naming compatibility.
        aliases.put("电影感写实", DEFAULT_STYLE_KEY);
        aliases.put("电影感", DEFAULT_STYLE_KEY);
        aliases.put("写实电影", DEFAULT_STYLE_KEY);
        aliases.put("cinematic", DEFAULT_STYLE_KEY);
        aliases.put("cinematic-realistic", DEFAULT_STYLE_KEY);
        aliases.put("realistic-cinematic", DEFAULT_STYLE_KEY);

        // React/Vue style library preset IDs.
        aliases.put("oriental-ink-wash", "ink-wash");
        aliases.put("oriental-classic-elegance", "ancient-chinese");
        aliases.put("oriental-cyber-dunhuang", "cyber-dunhuang");
        aliases.put("scifi-cyberpunk", "cyberpunk");
        aliases.put("scifi-futurism", "futurism");
        aliases.put("retro-hk", "retro-hk");
        aliases.put("retro-8mm", "retro-8mm");
        aliases.put("anime-cel", "anime");
        aliases.put("anime-pixel", "pixel-art");
        aliases.put("anime-puppet", "2d-animation");
        aliases.put("anime-chibi", "2d-animation");
        aliases.put("film-cinematic", "live-action");
        aliases.put("film-documentary", "film-documentary");
        aliases.put("film-noir", "film-noir");
        aliases.put("film-commercial", "film-commercial");
        aliases.put("art-oil", "oil-painting");
        aliases.put("art-watercolor", "watercolor");
        aliases.put("misc-vaporwave", "vaporwave");
        aliases.put("misc-nordic", "nordic");
        aliases.put("misc-gothic", "gothic-dark");
        aliases.put("misc-pop-art", "pop-art");

        // Chinese alias compatibility from presets.
        aliases.put("水墨风", "ink-wash");
        aliases.put("古风雅韵", "ancient-chinese");
        aliases.put("赛博敦煌", "cyber-dunhuang");
        aliases.put("赛博朋克", "cyberpunk");
        aliases.put("未来主义", "futurism");
        aliases.put("复古港风", "retro-hk");
        aliases.put("复古胶片", "retro-film");
        aliases.put("8mm家庭录像", "retro-8mm");
        aliases.put("8mm 家庭录像", "retro-8mm");
        aliases.put("二次元动漫", "anime");
        aliases.put("像素风格", "pixel-art");
        aliases.put("木偶动画", "2d-animation");
        aliases.put("q版卡通", "2d-animation");
        aliases.put("q 版卡通", "2d-animation");
        aliases.put("纪录片", "film-documentary");
        aliases.put("黑色电影", "film-noir");
        aliases.put("商业广告", "film-commercial");
        aliases.put("油画质感", "oil-painting");
        aliases.put("水彩风格", "watercolor");
        aliases.put("蒸汽波", "vaporwave");
        aliases.put("北欧清新", "nordic");
        aliases.put("暗黑哥特", "gothic-dark");
        aliases.put("波普艺术", "pop-art");

        return aliases;
    }
}
