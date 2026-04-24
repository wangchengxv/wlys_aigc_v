# OneLinkAI `doubao-seedance-2.0` 模型基座接入计划

## 一、Summary
- 目标：在现有 `onelinkai` 体系中新增 `doubao-seedance-2.0`，并支持按你给出的 OneLink 请求结构直接调用（`content` 多参考 + `generate_audio/ratio/duration/watermark`）。
- 范围：后端完整接入、保留现有 `doubao-seedance-1.5-pro` 兼容路径、前端同步可选模型与默认文案（不引入破坏性变更）。
- 成功标准：
  - 输入 OneLink API Key 后，可在预置模型中直接配置并调用 `doubao-seedance-2.0`。
  - 后端向 OneLink 提交请求时可按示例组装 `content`（`text/reference_image/reference_video/reference_audio`）与核心参数。
  - 现有 `doubao-seedance-1.5-pro` 调用与测试继续通过。

## 二、Current State Analysis

### 2.1 后端模型注册与路由现状
- `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
  - 已有 `onelinkai + doubao-seedance-1.5-pro`，暂无 `doubao-seedance-2.0`。
- `aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
  - `onelinkai` 静态模型列表未包含 `doubao-seedance-1.5-pro`/`2.0`，对连接测试与模型列表展示不完整。
- `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - `ONELINK_DOUBAO_VIDEO_MODEL` 固定为单值 `doubao-seedance-1.5-pro`。
  - `callOneLinkDoubaoVideoApi(...)` 当前仅提交：
    - `model`
    - `content: [text, image_url]`
  - 未支持 `reference_video`、`reference_audio`、多图参考、`generate_audio`、`ratio`、显式 `duration` 与 `watermark` 请求字段。
  - 轮询路径已独立为 `/volc/api/v3/contents/generations/tasks/{taskId}`，可复用。

### 2.2 前端现状
- `aigc-site-react/src/api/index.ts`
  - `MOCK_PRESET_MODELS` 中无 `onelinkai + doubao-seedance-2.0`。
- `aigc-site-react/src/components/workspace/PromptPanel.tsx`
  - 视频模型默认与占位文案仍偏向旧模型示例。
- `aigc-site-react/src/lib/workspace/advancedMedia.ts`
  - `GenerateAdvancedVideoRequest` 仅有 `referenceImageUrl`、`viduOptions` 与通用 `extra`，可承载扩展但缺少 OneLink Seedance 2.0 的明确结构约束与构建逻辑。

### 2.3 现有测试基础
- `aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
  - 已覆盖 OneLink 豆包 1.5 路由与基础 payload 校验。
  - 未覆盖 2.0 模型识别、多参考内容与新增参数透传。

## 三、Proposed Changes

### 3.1 后端：新增 2.0 预置与提供商静态模型
- 文件：`aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
  - 新增 `onelinkai + doubao-seedance-2.0` 预置项（`capabilities=["video"]`）。
  - 保留 `doubao-seedance-1.5-pro`。
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
  - 在 `onelinkai` `staticModels` 增加 `doubao-seedance-1.5-pro`、`doubao-seedance-2.0`，保证模型列表与连通性测试一致。

### 3.2 后端：OneLink Seedance 视频请求体升级（完整对齐示例）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - 将单模型常量改为模型集合识别（至少包含 `doubao-seedance-1.5-pro` 与 `doubao-seedance-2.0`）。
  - 扩展 `NormalizedAdvancedMedia`，新增 OneLink Seedance 专属视频扩展字段承载（来源于 `advancedMedia.video.extra`）。
  - 新增/调整 payload 构建逻辑：
    - `model`: 使用当前选中的 OneLink 豆包视频模型（1.5 或 2.0），不再写死单值。
    - `content`: 组装为可变列表，支持：
      - 文本节点：`{type:"text", text:"..."}`
      - 图片参考：`{type:"image_url", image_url:{url:"..."}, role:"reference_image"}`
      - 视频参考：`{type:"video_url", video_url:{url:"..."}, role:"reference_video"}`
      - 音频参考：`{type:"audio_url", audio_url:{url:"..."}, role:"reference_audio"}`
    - 透传可选参数：`generate_audio`、`ratio`、`duration`、`watermark`。
  - 兼容策略：
    - 保留当前 `videoReferenceImageUrl` 入口，未提供 `extra` 时保持原行为可用。
    - 继续走现有 OneLink 提交/轮询路径，不改接口协议与错误映射。
  - 校验策略（与现有风格一致）：
    - 参考媒体 URL 必须为可访问的 `http(s)`。
    - 至少保证一个文本内容；图片参考不足时沿用 `videoReferenceImageUrl` 兜底（避免旧调用断裂）。

### 3.3 前端：同步 2.0 模型可选项与请求结构承载
- 文件：`aigc-site-react/src/api/index.ts`
  - `MOCK_PRESET_MODELS` 增加 `onelinkai + doubao-seedance-2.0`，便于离线/Mock 场景直接可选。
- 文件：`aigc-site-react/src/types/index.ts`
  - 为 `GenerateAdvancedVideoRequest.extra` 补充 OneLink Seedance 2.0 结构化类型（多参考与参数字段），避免调用方使用纯 `unknown`。
- 文件：`aigc-site-react/src/lib/workspace/advancedMedia.ts`
  - 增加 OneLink Seedance 额外字段构建/透传辅助逻辑（保持向后兼容）。
- 文件：`aigc-site-react/src/components/workspace/PromptPanel.tsx`
  - 更新视频模型示例文案与默认提示，纳入 `doubao-seedance-2.0`。
  - 保持现有 UI 主流程；高级字段可通过 `advancedMedia.video.extra` 承载（不强制新增复杂表单，优先保证可调用与兼容）。

### 3.4 测试与回归
- 文件：`aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
  - 新增/调整用例：
    - `doubao-seedance-2.0` 能命中 OneLink 豆包视频专用分支。
    - 构造包含多参考（image/video/audio）与参数的高级请求后，提交 payload 结构符合预期。
    - 1.5 旧用例保持通过（兼容验证）。
- 若前端项目已有对应单测框架，则补一条轻量类型/构建函数测试；若无现成模式则以手工联调为主。

## 四、Assumptions & Decisions
- 已确认决策：
  - 接入档位：完整对齐示例 payload。
  - 版本策略：保留 1.5，同时新增 2.0。
  - 改动范围：后端 + 前端同步调整。
- 设计决策：
  - 不新增独立 API，沿用现有 `generate` 入口，通过 `advancedMedia.video.extra` 承载 2.0 扩展字段。
  - 不移除现有 `videoReferenceImageUrl`，以确保历史调用与现有工作台兼容。
  - OneLink 豆包视频仍复用现有提交与轮询路径（`/volc/api/v3/contents/generations/tasks`）。

## 五、Verification Steps

### 5.1 后端验证
- 执行 `GenerationServiceImplViduTest`，确认新增与存量用例通过。
- 手工调用 `/api/v1/generate`（`mode=video`）：
  - 使用 `videoModel=doubao-seedance-2.0` + OneLink 连接。
  - 在 `advancedMedia.video.extra` 中传入多参考与参数，确认服务端提交体正确并返回视频结果。
- 回归 `videoModel=doubao-seedance-1.5-pro`，确认旧调用不中断。

### 5.2 前端验证
- 快捷配置/模型选择中可看到 `doubao-seedance-2.0`（真实接口与 Mock 场景）。
- 工作台提交视频任务后，请求体包含约定的 `advancedMedia.video.extra` 字段（当用户填写时）。
- 不填写扩展字段时，原有视频提交流程保持可用。

### 5.3 风险检查
- 校验扩展字段不会污染 Vidu/Kling/Moark 分支。
- 校验默认模型与占位文案调整不影响已有项目草稿载入与回填。
