# onelinkai 厂商豆包模型改造计划

## Summary
- 目标：将 `onelinkai` 厂商下的豆包视频模型（仅精确匹配 `doubao-seedance-1.5-pro`）改造成按你提供的 OneLink 请求格式提交任务。
- 目标请求形态：`POST https://api.onelinkai.cloud/volc/api/v3/contents/generations/tasks`，`Authorization: Bearer <连接API Key>`，`Content-Type: application/json`，请求体包含 `model` 与 `content=[text,image_url]`。
- 成功标准：
  - Java 与 Go 两套后端都在 `onelinkai + doubao-seedance-1.5-pro` 时走新链路；
  - 请求头继续使用连接配置里的 API Key（Bearer）；
  - `image_url` 必填，缺失时返回 400 业务错误；
  - 不影响现有 OneLink 的 Vidu/Kling 路径。

## Current State Analysis
- Java 端：
  - 视频主分发在 `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java` 的 `generateVideosWithConfiguredModel` 与 `generateVideosWithOneLinkConnection`。
  - 当前 `onelinkai` 仅支持两类视频模型：Vidu（`isViduWorkspaceModel`）与 Kling（`isKlingModel`），不支持 `doubao-seedance-1.5-pro`。
  - Ark（方舟）视频的请求体构造在 `callArkVideoApi`：`content` 目前只有 `text` 节点，文本拼接 `--duration ... --camerafixed false --watermark ...`。
  - `ProviderCatalog` 里 `onelinkai` 默认 `AuthMode.BEARER`，基础域名 `https://api.onelinkai.cloud`；`ark` 的视频路径是 `/api/v3/contents/generations/tasks`，未带 `/volc` 前缀。
- Go 端：
  - 视频主分发在 `aigc-server-go/internal/service/generation.go` 的 `genVideos`。
  - `rm.Provider.Key == "onelinkai"` 分支同样仅支持 Vidu/Kling，其他模型直接报错。
  - Ark 视频请求构造在 `callArkVideo`，与 Java 一样只发 `content=[text]`。
  - 网关 POST/GET 在 `aigc-server-go/internal/gateway/http.go`，`AuthBearer` 已是 `Authorization: Bearer <apiKey>`。
- 预设模型：
  - Java `PresetModelRegistry` 与 Go `preset_models.go` 当前都没有 `onelinkai + doubao-seedance-1.5-pro` 预设项。

## Proposed Changes

### 1) Java：新增 OneLink 豆包视频专用分支（核心行为）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 变更：
  - 在 `generateVideosWithOneLinkConnection` 增加精确匹配：当模型名（忽略大小写）等于 `doubao-seedance-1.5-pro` 时，走新的 OneLink 豆包视频调用方法；其余保持现有 Vidu/Kling 判定。
  - 新增私有方法（如 `isOneLinkDoubaoVideoModel`、`generateVideosWithOneLinkDoubaoConnection`、`callOneLinkDoubaoVideoApi`）：
    - 强制要求 `videoReferenceImageUrl` 非空且可用（沿用现有 URL 校验逻辑）；
    - 组装请求体：
      - `model: "doubao-seedance-1.5-pro"`
      - `content` 两段：
        - `{"type":"text","text":"<prompt> --duration {safeVideoDuration} --camerafixed false --watermark {arkProperties.isWatermark()}"}`
        - `{"type":"image_url","image_url":{"url":"<referenceImageUrl>"}}`
    - 调用路径固定为 `/volc/api/v3/contents/generations/tasks`（通过 `providerHttpGateway.postJson` 直接指定 path）；
    - 任务查询路径使用 `/volc/api/v3/contents/generations/tasks/{taskId}` 轮询（复用现有轮询框架与解析逻辑）。
- 原因：
  - 与你给出的 Unirest 示例保持结构一致；
  - 避免改动 Ark 或其它 provider 的通用路径，降低回归风险。

### 2) Go：对齐 Java 的 OneLink 豆包专用分支
- 文件：`aigc-server-go/internal/service/generation.go`
- 变更：
  - 在 `genVideos` 的 `onelinkai` 分支中，新增精确匹配 `doubao-seedance-1.5-pro` 的处理优先级（高于现有“仅 Vidu/Kling”报错）。
  - 新增方法（如 `isOneLinkDoubaoVideoModel`、`callOneLinkDoubaoVideo`）：
    - 强制要求 `refURL` 必填；
    - 组装与 Java 等价的请求体（`content=[text,image_url]`）；
    - 通过 `g.GW.PostJSON` 调 `/volc/api/v3/contents/generations/tasks`；
    - 轮询 `/volc/api/v3/contents/generations/tasks/{taskId}` 并复用现有 URL 解析逻辑。
- 原因：
  - 你要求 Java+Go 双端改造；
  - 统一两后端行为，避免环境切换导致功能不一致。

### 3) 预设模型补充（可发现性）
- 文件：
  - `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
  - `aigc-server-go/internal/service/preset_models.go`
- 变更：
  - 新增 `onelinkai` 厂商下 `doubao-seedance-1.5-pro` 的视频能力预设项（`caps = ["video"]`），展示名标注为 OneLink 豆包视频模型。
- 原因：
  - 让管理台/初始化模型列表可直接选中该模型，减少手工配置出错。

### 4) 单测补强（锁定新增行为）
- 文件：
  - `aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
  - `aigc-server-go/internal/service/generation_test.go`
- 变更：
  - Java：
    - 新增 `onelinkai + doubao-seedance-1.5-pro` 触发新路径的测试；
    - 断言提交 payload 包含 `content` 的 `text` + `image_url` 两段；
    - 断言 path 为 `/volc/api/v3/contents/generations/tasks`；
    - 断言缺少 `videoReferenceImageUrl` 时抛 400。
  - Go：
    - 新增同等单测：校验模型匹配与 payload 结构；
    - 补充缺图报错测试。
- 原因：
  - 防止后续改动把 OneLink 豆包链路回退为旧行为或误入 Vidu/Kling 分支。

## Assumptions & Decisions
- 决策：改动范围为 Java + Go 两个后端。
- 决策：鉴权沿用连接配置 API Key，不硬编码任何密钥。
- 决策：仅模型名精确匹配 `doubao-seedance-1.5-pro` 时启用新链路，不做 `seedance*` 前缀泛化。
- 决策：该模型必须提供参考图，按 `image_url` 结构提交；无图直接报 400。
- 假设：OneLink `/volc/api/v3/contents/generations/tasks` 的查询也遵循 `/{taskId}` 路径，返回体可沿用现有 task_id 与视频 URL 解析逻辑。

## Verification Steps
- Java 定向测试：
  - 执行 `GenerationServiceImplViduTest`，重点关注新增 OneLink 豆包用例。
- Go 定向测试：
  - 执行 `generation_test.go`，重点关注新增模型匹配、payload 与缺图校验用例。
- 代码静态核对：
  - 搜索 `doubao-seedance-1.5-pro`，确认仅在 onelinkai 分支触发；
  - 搜索 `/volc/api/v3/contents/generations/tasks`，确认 Java/Go 均已接入；
  - 搜索 `image_url`，确认请求体含参考图节点且来源于 `videoReferenceImageUrl`。
- 手工联调（可选）：
  - 使用 onelinkai 连接 + `doubao-seedance-1.5-pro` + 参考图发起视频任务，确认提交成功并可轮询出视频地址。
