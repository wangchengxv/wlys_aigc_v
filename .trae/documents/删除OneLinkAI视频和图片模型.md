# OneLinkAI 模型清理计划

## 任务目标
从 `PresetModelRegistry.java` 中删除 OneLinkAI 的视频模型和图片模型，仅保留文本模型。

## 当前 OneLinkAI 相关模型分析

### 1. `onelinkai` provider (第 15-32 行)

**保留（文本模型）：**
- Line 15: `gpt-4o` - text
- Line 16: `claude-sonnet-4-6` - text  
- Line 17: `gemini-2.5-pro` - text

**删除（图片模型）：**
- Line 18: `wanx-v1` - image
- Line 19: `video-kling-v3` - image
- Line 20: `video-kling-v3` - image

**删除（视频模型）：**
- Line 21: `MiniMax-M2.1` - video
- Line 22: `video-kling-v3-6` - video
- Line 23: `kling-v1` - video
- Line 24: `viduq3-turbo` - video
- Line 25: `viduq3-pro` - video
- Line 32: `vidu2.0` - video

**删除（图片+视频模型）：**
- Line 26: `image-vidu-q2-fast` - image, video
- Line 27: `image-vidu-q2` - image, video
- Line 28: `viduq2-turbo` - image, video
- Line 29: `viduq2` - image, video
- Line 30: `viduq1` - image, video
- Line 31: `viduq1-classic` - image, video

### 2. `vidu_onelink` provider (第 62-71 行) - 全部删除

全部为图片/视频相关模型：
- Line 62: `viduq3-turbo` - video
- Line 63: `viduq3-pro` - video
- Line 64: `viduq3-pro-img2video` - image, video
- Line 65: `image-vidu-q2-fast` - image, video
- Line 66: `image-vidu-q2` - image, video
- Line 67: `viduq2-turbo` - image, video
- Line 68: `viduq2` - image, video
- Line 69: `viduq1` - image, video
- Line 70: `viduq1-classic` - image, video
- Line 71: `vidu2.0` - video

### 3. `kling` provider (第 72-75 行) - 全部删除

虽然 provider 名是 `kling`，但所有模型都使用 `https://api.onelinkai.cloud`，属于 OneLinkAI 的视频模型：
- Line 72: `video-kling-v3-6` - video
- Line 73: `video-kling-v3` - video
- Line 74: `kling-v1-6` - video
- Line 75: `kling-v1` - video

## 实施步骤

1. **删除 `onelinkai` provider 的图片模型**（Lines 18-20）
   - `wanx-v1`, `video-kling-v3`, `video-kling-v3`

2. **删除 `onelinkai` provider 的视频模型**（Lines 21-25, 32）
   - `MiniMax-M2.1`, `video-kling-v3-6`, `kling-v1`, `viduq3-turbo`, `viduq3-pro`, `vidu2.0`

3. **删除 `onelinkai` provider 的图片+视频模型**（Lines 26-31）
   - `image-vidu-q2-fast`, `image-vidu-q2`, `viduq2-turbo`, `viduq2`, `viduq1`, `viduq1-classic`

4. **删除整个 `vidu_onelink` provider**（Lines 62-71）

5. **删除整个 `kling` provider**（Lines 72-75）

6. **保留的 OneLinkAI 相关模型**：
   - `onelinkai` provider: `gpt-4o`, `claude-sonnet-4-6`, `gemini-2.5-pro` (均为文本模型)

## 预期结果

清理后，OneLinkAI 相关配置将仅包含 3 个文本模型。
