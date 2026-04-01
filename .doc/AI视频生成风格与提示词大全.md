# 视觉风格锚点词典｜完整增强版

> **使用规则**：风格重复时，以本版本为准（已基于扩写润色稿全面升级）。  
> 适配平台：通义万相 / 可灵（中文）· Runway Gen-3 / Pika 2.0 / Veo（英文）

---

## 1) 视频模板中的风格句

### 普通视频（中文）
```
视觉风格锚点：{visualStyle}
- 视觉风格：电影质感，保持一致的光照与色调
```

### 普通视频（英文）
```
Visual Style Anchor: {visualStyle}
- Visual Style: Cinematic quality with consistent lighting and color tone throughout
```

### 九宫格视频（中文）
```
视觉风格锚点：{visualStyle}
保持角色外观一致与电影质感。可{language}配音/旁白，但禁止字幕与任何画面文字。
```

### 九宫格视频（英文）
```
Visual Style Anchor: {visualStyle}
Maintain character consistency and cinematic quality throughout all panels.
```

### Veo 模式（首帧 / 首尾帧）
```
Visual Style Anchor: {visualStyle}
```

---

## 2) 视觉风格锚点词典

> 共 **14 种 style key**，原有 6 种已升级，新增 8 种。

---

### ① `live-action` · 真人实拍

**英文注入词：**
```
photorealistic live-action footage, cinematic film quality, real human actors with natural skin texture and subsurface scattering, professional cinematography with anamorphic lens characteristics, natural mixed lighting with motivated sources, 8K resolution, shallow depth of field with creamy bokeh, subtle film grain texture, cinematic color grade, anamorphic lens flare on backlit edges, three-point lighting setup, ultra-smooth camera motion, no CGI artifacts
```

**中文简描：**
```
真人实拍电影风格，photorealistic，8K高清，专业摄影，浅景深，自然光源，胶片颗粒感，电影级调色
```

---

### ② `anime` · 日本动漫

**英文注入词：**
```
Japanese anime style, cel-shaded flat coloring with precise shadow bands, vibrant saturated color palette, large expressive eyes with multi-layered iris highlights and specular dots, dynamic action poses with exaggerated motion blur streaks, clean sharp outlines with consistent line weight, flowing hair with defined highlight ribbons, Studio Ghibli / Makoto Shinkai production quality, hand-painted atmospheric sky backgrounds with soft cloud gradients, soft ambient fill light with dramatic colored rim light on character edges, sakura or particle effects for atmosphere
```

**中文简描：**
```
日本动漫风格，cel-shaded，鲜艳色彩，大眼睛高光，Studio Ghibli / 新海诚品质，手绘天空背景，动感轮廓线
```

---

### ③ `2d-animation` · 经典2D动画

**英文注入词：**
```
classic 2D hand-drawn animation, Disney golden-age / Pixar 2D hybrid quality, smooth clean lines with consistent stroke weight, expressive character acting using squash-and-stretch and anticipation principles, painterly watercolor or gouache backgrounds with visible brushwork, soft gradient shading with warm color palette, round friendly proportions with appeal-driven design, fluid 24fps animation timing, secondary motion on hair and clothing
```

**中文简描：**
```
经典2D手绘动画，Disney/Pixar品质，流畅线条，夸张表情动作，水彩背景，暖色调，角色比例圆润亲和
```

---

### ④ `3d-animation` · 3D CGI动画

**英文注入词：**
```
high-quality 3D CGI animation, Pixar / DreamWorks production quality, stylized character proportions with appealing design, subsurface scattering on skin and organic materials, detailed physically-based rendering (PBR) textures, volumetric lighting with soft area light shadows, ambient occlusion in crevices, global illumination color bleed, smooth motion blur on fast movement, expressive facial rigging with blend shape detail, polished environment shading with depth haze
```

**中文简描：**
```
3D CGI动画，Pixar/DreamWorks风格，PBR材质，次表面散射，体积光，全局光照，精细面部绑定
```

---

### ⑤ `cyberpunk` · 赛博朋克

**英文注入词：**
```
cyberpunk aesthetic, rain-soaked neon-lit urban night environment, reflective wet asphalt mirroring layered holographic signage, volumetric fog with neon color bleeding and light shaft penetration, high-tech low-life contrast, Blade Runner 2049 cinematic style, chromatic aberration on lens edges, cool blue-purple dominant palette with hot pink and cyan accent neons, gritty micro-detail textures on worn surfaces, lens flare from point light sources, holographic UI overlays with glitch artifacts, shallow depth of field
```

**中文简描：**
```
赛博朋克美学，雨夜霓虹，湿地反光，体积雾，Blade Runner风格，蓝紫主色调，全息UI，色差效果
```

---

### ⑥ `oil-painting` · 油画质感

**英文注入词：**
```
classical oil painting style, visible directional impasto brushstrokes with palette knife texture, rich multi-layer glazing depth, warm chromatic undertones, museum-quality fine art composition following the golden ratio, Rembrandt three-quarter dramatic lighting, strong chiaroscuro contrast between illuminated and shadow zones, canvas weave texture visible throughout, painterly edge treatment with lost-and-found contours, Baroque color richness with deep shadow detail
```

**中文简描：**
```
古典油画风格，可见厚涂笔触，多层釉色深度，伦勃朗光，明暗对比强，画布纹理，巴洛克色彩，边缘虚实处理
```

---

### ⑦ `ink-wash` · 水墨国风

**英文注入词：**
```
traditional Chinese ink-wash painting style, sumi-e technique, ink bleeding naturally into absorbent rice paper ground, rich tonal gradation from dense black to translucent grey washes, deliberate generous negative space (留白) for East Asian compositional breathing room, lost soft edges where ink diffuses into damp paper, monochromatic blue-grey atmospheric palette with occasional muted earth accent, brushwork texture visible at stroke edges, poetic misty depth layers, minimal color — ink tone variation carries all narrative weight
```

**中文简描：**
```
中国水墨画风格，墨迹自然晕染，浓淡干湿层次，大量留白，宣纸质感，蓝灰色调，东方意境，笔触可见
```

---

### ⑧ `ancient-chinese` · 古风雅韵

**英文注入词：**
```
classical Chinese aesthetic (古风), Tang-Song dynasty visual language, traditional hanfu garments with layered silk fabric and wide sleeves, soft directional light filtered through rice-paper screens, symmetrical architectural framing with carved wooden lattice and stone lanterns, warm ivory and ink-green and ochre-red color palette, falling petals and flowing water as atmospheric elements, unhurried meditative pacing, no modern materials or objects, painterly soft-focus background treatment, shallow depth of field on fabric texture detail
```

**中文简描：**
```
中国古风美学，汉服广袖，米黄墨绿赭石色调，宣纸透光感，对称构图，花瓣飘落，无现代元素，意境悠远
```

---

### ⑨ `cyber-dunhuang` · 赛博敦煌

**英文注入词：**
```
Cyber-Dunhuang fusion aesthetic, Tang Dynasty Apsara celestial figures reimagined in digital neon space, traditional Dunhuang fresco motifs (lotus, cloud scrolls, flying ribbon sashes) rendered in holographic pixel fragments, deep violet and electric blue and burnished gold color palette, neon particle streams echoing ribbon dance movement, zero-gravity floating architecture referencing Buddhist cave-temple geometry, glitch artifacts on ancient pattern transitions, volumetric light shafts through digital void, high contrast between warm traditional pigment tones and cold neon technology
```

**中文简描：**
```
赛博敦煌融合，飞天元素数字化，传统壁画纹样像素化重组，深紫宝蓝敦煌金，全息粒子，无重力飘带，冷暖对撞
```

---

### ⑩ `watercolor` · 水彩手绘

**英文注入词：**
```
hand-painted watercolor illustration style, transparent pigment washes bleeding softly into damp paper, visible paper grain and watermark texture throughout, wet-on-wet background treatment with color blooming at edges, dry-brush detail strokes for foreground elements, Morandi-inspired desaturated pastel palette (soft pink, pale blue, sage green, off-white), natural color pooling and tide-mark edges where washes dried, no hard black outlines — edges defined only by tonal contrast, light and airy composition with generous white paper showing through
```

**中文简描：**
```
手绘水彩风格，颜料透明晕染，纸纹水痕可见，湿画法背景，干笔法细节，莫兰迪低饱和马卡龙色，无硬轮廓
```

---

### ⑪ `retro-film` · 复古胶片

**英文注入词：**
```
vintage analog film aesthetic, Kodak 5219 or Fuji 500T film stock simulation, visible silver-halide grain structure with natural random distribution, slightly desaturated color with warm orange-red midtone shift, blue-shifted shadows, gentle vignette darkening at frame edges, soft halation bloom around highlights and backlit edges, occasional subtle lens flare streaks, natural lens distortion with mild barrel effect at wide angle, no digital sharpening, no HDR processing, no clean edges — authentic analog imperfection throughout
```

**中文简描：**
```
复古胶片风格，柯达5219模拟，银盐颗粒感，轻微去饱和，暖橙中间调，蓝移暗部，边缘暗角，高光晕散，无数字锐化
```

---

### ⑫ `gothic-dark` · 暗黑哥特

**英文注入词：**
```
dark Gothic aesthetic, medieval cathedral or Victorian interior setting, extreme high-contrast chiaroscuro with deep shadowless blacks, single warm amber candlelight or torch as the only motivated light source, stone or aged wood textures with lichen and weathering detail, heavy velvet drapery with embroidered motifs, slow deliberate movement with oppressive atmospheric stillness, cool shadow tones against isolated warm light pools, no fill light — intentional large shadow areas, mysterious and airless mood, muted desaturated palette except for the warm flame source
```

**中文简描：**
```
暗黑哥特风格，中世纪或维多利亚场景，极端明暗对比，烛光唯一光源，石材/天鹅绒质感，大面积纯黑阴影，压抑神秘氛围
```

---

### ⑬ `vaporwave` · 蒸汽波

**英文注入词：**
```
vaporwave aesthetic, 1980s retro-futurist digital nostalgia, infinite purple-pink grid floor receding to horizon, magenta-to-violet gradient sky, floating Greco-Roman marble bust with intentional damage and museum patina, vintage beige CRT monitor screens flickering with pixelated waveforms and retro geometric loops, pixelated palm tree silhouettes in bright orange outline, high-saturation complementary color clash (deep purple, magenta, electric blue, coral-orange), VHS scan line overlay artifact, no anti-aliasing, deliberate lo-fi digital texture
```

**中文简描：**
```
蒸汽波美学，80年代复古数字感，紫粉网格地板，古希腊雕像残件，CRT显示器，棕榈树剪影，高饱和撞色，VHS扫描线
```

---

### ⑭ `pixel-art` · 像素风格

**英文注入词：**
```
retro 8-bit pixel art style, chunky visible pixel blocks with no anti-aliasing, deliberately limited color palette per sprite, hard pixel-perfect edges on all elements, classic video game sprite proportions, scanline texture overlay for CRT monitor feel, deliberately low frame rate animation (12fps) for authentic retro movement, bright punchy saturated colors (electric blue, neon green, lemon yellow), blocky terrain and environment tiles, sprite-based character design with minimal pixel detail, chiptune aesthetic visual language
```

**中文简描：**
```
8bit像素游戏风格，像素块清晰无抗锯齿，有限色板，CRT扫描线，12fps低帧率动画，高饱和纯色，方块地形，精灵图角色
```

---

## 3) 可直接复制版（已替换示例）

### 通用占位模板
```
视觉风格锚点：{visualStyle}
```
```
Visual Style Anchor: {visualStyle}
```

---

### `live-action` 真人实拍
```
Visual Style Anchor: photorealistic live-action footage, cinematic film quality, real human actors with natural skin texture and subsurface scattering, professional cinematography with anamorphic lens characteristics, natural mixed lighting with motivated sources, 8K resolution, shallow depth of field with creamy bokeh, subtle film grain texture, cinematic color grade, anamorphic lens flare on backlit edges, three-point lighting setup, ultra-smooth camera motion, no CGI artifacts
```

### `anime` 日本动漫
```
Visual Style Anchor: Japanese anime style, cel-shaded flat coloring with precise shadow bands, vibrant saturated color palette, large expressive eyes with multi-layered iris highlights and specular dots, dynamic action poses with exaggerated motion blur streaks, clean sharp outlines with consistent line weight, flowing hair with defined highlight ribbons, Studio Ghibli / Makoto Shinkai production quality, hand-painted atmospheric sky backgrounds with soft cloud gradients, soft ambient fill light with dramatic colored rim light on character edges, sakura or particle effects for atmosphere
```

### `2d-animation` 经典2D动画
```
Visual Style Anchor: classic 2D hand-drawn animation, Disney golden-age / Pixar 2D hybrid quality, smooth clean lines with consistent stroke weight, expressive character acting using squash-and-stretch and anticipation principles, painterly watercolor or gouache backgrounds with visible brushwork, soft gradient shading with warm color palette, round friendly proportions with appeal-driven design, fluid 24fps animation timing, secondary motion on hair and clothing
```

### `3d-animation` 3D CGI动画
```
Visual Style Anchor: high-quality 3D CGI animation, Pixar / DreamWorks production quality, stylized character proportions with appealing design, subsurface scattering on skin and organic materials, detailed physically-based rendering (PBR) textures, volumetric lighting with soft area light shadows, ambient occlusion in crevices, global illumination color bleed, smooth motion blur on fast movement, expressive facial rigging with blend shape detail, polished environment shading with depth haze
```

### `cyberpunk` 赛博朋克
```
Visual Style Anchor: cyberpunk aesthetic, rain-soaked neon-lit urban night environment, reflective wet asphalt mirroring layered holographic signage, volumetric fog with neon color bleeding and light shaft penetration, high-tech low-life contrast, Blade Runner 2049 cinematic style, chromatic aberration on lens edges, cool blue-purple dominant palette with hot pink and cyan accent neons, gritty micro-detail textures on worn surfaces, lens flare from point light sources, holographic UI overlays with glitch artifacts, shallow depth of field
```

### `oil-painting` 油画质感
```
Visual Style Anchor: classical oil painting style, visible directional impasto brushstrokes with palette knife texture, rich multi-layer glazing depth, warm chromatic undertones, museum-quality fine art composition following the golden ratio, Rembrandt three-quarter dramatic lighting, strong chiaroscuro contrast between illuminated and shadow zones, canvas weave texture visible throughout, painterly edge treatment with lost-and-found contours, Baroque color richness with deep shadow detail
```

### `ink-wash` 水墨国风
```
Visual Style Anchor: traditional Chinese ink-wash painting style, sumi-e technique, ink bleeding naturally into absorbent rice paper ground, rich tonal gradation from dense black to translucent grey washes, deliberate generous negative space for East Asian compositional breathing room, lost soft edges where ink diffuses into damp paper, monochromatic blue-grey atmospheric palette with occasional muted earth accent, brushwork texture visible at stroke edges, poetic misty depth layers, minimal color — ink tone variation carries all narrative weight
```

### `ancient-chinese` 古风雅韵
```
Visual Style Anchor: classical Chinese aesthetic, Tang-Song dynasty visual language, traditional hanfu garments with layered silk fabric and wide sleeves, soft directional light filtered through rice-paper screens, symmetrical architectural framing with carved wooden lattice and stone lanterns, warm ivory and ink-green and ochre-red color palette, falling petals and flowing water as atmospheric elements, unhurried meditative pacing, no modern materials or objects, painterly soft-focus background treatment, shallow depth of field on fabric texture detail
```

### `cyber-dunhuang` 赛博敦煌
```
Visual Style Anchor: Cyber-Dunhuang fusion aesthetic, Tang Dynasty Apsara celestial figures reimagined in digital neon space, traditional Dunhuang fresco motifs rendered in holographic pixel fragments, deep violet and electric blue and burnished gold color palette, neon particle streams echoing ribbon dance movement, zero-gravity floating architecture referencing Buddhist cave-temple geometry, glitch artifacts on ancient pattern transitions, volumetric light shafts through digital void, high contrast between warm traditional pigment tones and cold neon technology
```

### `watercolor` 水彩手绘
```
Visual Style Anchor: hand-painted watercolor illustration style, transparent pigment washes bleeding softly into damp paper, visible paper grain and watermark texture throughout, wet-on-wet background treatment with color blooming at edges, dry-brush detail strokes for foreground elements, Morandi-inspired desaturated pastel palette, natural color pooling and tide-mark edges where washes dried, no hard black outlines — edges defined only by tonal contrast, light and airy composition with generous white paper showing through
```

### `retro-film` 复古胶片
```
Visual Style Anchor: vintage analog film aesthetic, Kodak 5219 film stock simulation, visible silver-halide grain structure with natural random distribution, slightly desaturated color with warm orange-red midtone shift, blue-shifted shadows, gentle vignette darkening at frame edges, soft halation bloom around highlights and backlit edges, occasional subtle lens flare streaks, natural lens distortion with mild barrel effect at wide angle, no digital sharpening, no HDR processing, authentic analog imperfection throughout
```

### `gothic-dark` 暗黑哥特
```
Visual Style Anchor: dark Gothic aesthetic, medieval cathedral or Victorian interior setting, extreme high-contrast chiaroscuro with deep shadowless blacks, single warm amber candlelight as the only motivated light source, stone or aged wood textures with lichen and weathering detail, heavy velvet drapery with embroidered motifs, slow deliberate movement with oppressive atmospheric stillness, cool shadow tones against isolated warm light pools, no fill light — intentional large shadow areas, mysterious and airless mood
```

### `vaporwave` 蒸汽波
```
Visual Style Anchor: vaporwave aesthetic, 1980s retro-futurist digital nostalgia, infinite purple-pink grid floor receding to horizon, magenta-to-violet gradient sky, floating Greco-Roman marble bust with intentional damage and museum patina, vintage beige CRT monitor screens flickering with pixelated waveforms, pixelated palm tree silhouettes in bright orange outline, high-saturation complementary color clash (deep purple, magenta, electric blue, coral-orange), VHS scan line overlay artifact, no anti-aliasing, deliberate lo-fi digital texture
```

### `pixel-art` 像素风格
```
Visual Style Anchor: retro 8-bit pixel art style, chunky visible pixel blocks with no anti-aliasing, deliberately limited color palette per sprite, hard pixel-perfect edges on all elements, classic video game sprite proportions, scanline texture overlay for CRT monitor feel, deliberately low frame rate animation (12fps) for authentic retro movement, bright punchy saturated colors, blocky terrain and environment tiles, sprite-based character design with minimal pixel detail, chiptune aesthetic visual language
```

---

## 4) 风格速查对照表

| style key | 中文名 | 适用场景 | 核心色调 |
|---|---|---|---|
| `live-action` | 真人实拍 | 剧情短片、广告、纪录片 | 自然色，电影调色 |
| `anime` | 日本动漫 | 动漫短片、MV、故事 | 高饱和，鲜艳色 |
| `2d-animation` | 经典2D动画 | 儿童内容、童话、品牌 | 暖色，水彩背景 |
| `3d-animation` | 3D CGI动画 | 科技、游戏、现代故事 | 全真实感，PBR |
| `cyberpunk` | 赛博朋克 | 科幻、未来、游戏宣传 | 蓝紫+品红霓虹 |
| `oil-painting` | 油画质感 | 艺术短片、史诗、人文 | 暖棕，伦勃朗光 |
| `ink-wash` | 水墨国风 | 东方意境、诗词、文化 | 蓝灰墨色，留白 |
| `ancient-chinese` | 古风雅韵 | 古装故事、国风品牌 | 米黄墨绿赭石 |
| `cyber-dunhuang` | 赛博敦煌 | 文化融合、创意广告 | 深紫宝蓝敦煌金 |
| `watercolor` | 水彩手绘 | 文艺、治愈、小清新 | 莫兰迪马卡龙 |
| `retro-film` | 复古胶片 | 怀旧、回忆、人文故事 | 暖橙，蓝移暗部 |
| `gothic-dark` | 暗黑哥特 | 悬疑、恐怖、神秘叙事 | 深黑+烛光琥珀 |
| `vaporwave` | 蒸汽波 | 音乐、创意、复古未来 | 紫粉+电蓝撞色 |
| `pixel-art` | 像素风格 | 游戏、复古科技、创意 | 高饱和纯色块 |

---

## 5) 禁忌词标准套装（每条提示词末尾附加）

**英文（Runway / Pika / Veo）：**
```
no clipping, no distortion, no extra limbs, no morphing faces, no motion blur artifacts, stabilized frame, natural fluid motion, no perspective errors, no duplicate subjects, no watermarks, no text overlays unless specified
```

**中文（通义万相 / 可灵）：**
```
无穿模，无畸形，无多余肢体，无人脸变形，无运动模糊瑕疵，画面稳定，动作自然流畅，无透视错误，无重复主体，无水印，无画面文字（特别指定除外）
```

---

*适配平台：通义万相 · 可灵 · Runway Gen-3 Alpha · Pika 2.0 · Google Veo 2 · Sora*