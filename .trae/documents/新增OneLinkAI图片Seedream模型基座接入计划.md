# 新增 OneLinkAI 图片 Seedream 模型基座接入计划

## Summary
- 目标：在 `onelinkai` 模型基座中新增 3 个图片模型 `doubao-seedream-4.0`、`doubao-seedream-4.5`、`doubao-seedream-5.0-lite`，并确保管理员配置 API 后可在文生图与剧本数字资产相关页面直接调用。
- 范围：仅修改 Java 后端 `aigc-server`（按确认不改 `aigc-server-go`）。
- 核心策略：
  - 预置/目录侧：把 3 个模型加入预置模型与服务商静态模型，保证“快捷模式 + 服务商中心”可见。
  - 生成调用侧：`onelinkai` 下所有 `seedream*` 图片模型统一改走 `POST /volc/api/v3/images/generations`，请求体按 OneLink 豆包图片格式发送。

## Current State Analysis
- 预置模型来源：
  - `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
  - 现状：`onelinkai` 已有 `wanx-v1`（image）及视频模型，未包含上述 3 个 seedream 图片模型。
- 服务商目录来源：
  - `aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
  - 现状：`onelinkai` 的 `staticModels` 未包含上述 3 个模型；目录用于服务商中心向导展示参考模型。
- 图片生成主链路：
  - `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - 现状：
    - 通用图片调用在 `generateImagesWithConfiguredModel` 中走 `providerHttpGateway.generateImage(...)`。
    - `onelinkai` 普通图片默认沿用 provider 的 `imageGenerationPath`（当前为 `/v1/images/generations`）与 OpenAI 兼容字段（`model/prompt/n/size/response_format`）。
    - 仅 OneLink 豆包视频已有专门 `volc` 路径与 payload（可复用其分流设计思想）。
- 前端可见性链路：
  - 快捷模式模型下拉取自 `/api/v1/preset-models`（`QuickModelForm.tsx`）。
  - 服务商中心向导参考模型取自 `/api/v1/provider-catalog` 的 `staticModels`（`AddProviderWizard.tsx`）。
  - 文生图/剧本资产页模型候选来自 `/api/v1/models/image`（`PromptPanel.tsx`、`WorkflowModelPanel.tsx`），本质依赖已配置且 capability 为 `image` 的模型。

## Proposed Changes

### 1) 预置模型补充（快捷模式可选）
- 文件：`aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
- 修改：
  - 为 `onelinkai` 新增 3 条 `PresetModel`，capabilities 均为 `List.of("image")`：
    - `doubao-seedream-4.0`
    - `doubao-seedream-4.5`
    - `doubao-seedream-5.0-lite`
- 原因：
  - 快捷模式依赖预置模型清单，未入库则无法“一键配置连接+模型”。
- 实现要点：
  - displayName 统一用 `OneLinkAI 豆包 Seedream ...` 风格，保持现有命名一致性。

### 2) 服务商目录静态模型补充（服务商中心可见）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
- 修改：
  - 在 `onelinkai` 的 `staticModels` 中追加上述 3 个模型 ID。
- 原因：
  - 服务商中心“添加服务商”向导会展示静态模型参考，便于管理员手动配置模型。
- 实现要点：
  - 不改 `onelinkai` 全局 `imageGenerationPath`，避免影响非 seedream 图片模型（如 `wanx-v1`）的现有行为。

### 3) OneLink Seedream 图片调用分流到 volc 接口
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 修改：
  - 新增 OneLink Seedream 图片接口常量：
    - `ONELINK_DOUBAO_IMAGE_PATH = "/volc/api/v3/images/generations"`
  - 新增模型识别逻辑（建议方法名）：
    - `isOneLinkSeedreamImageModel(String modelName)`：对 `seedream` 前缀/关键字做大小写不敏感判断。
  - 在 `generateImagesWithConfiguredModel(...)` 中增加分支：
    - 当 provider 为 `onelinkai` 且模型命中 `seedream*` 时，调用新私有方法（例如 `callOneLinkSeedreamImageApi(...)`）；
    - 其余模型继续沿用当前通用调用逻辑（含 Kling 分支和普通图片分支）。
  - 新私有方法按 OneLink 要求组装 payload 并请求：
    - endpoint：`/volc/api/v3/images/generations`
    - payload 字段：
      - `model`：当前模型名
      - `prompt`：提示词
      - `sequential_image_generation`：`"disabled"`
      - `response_format`：`"url"`
      - `size`：`"2K"`
      - `stream`：`false`
      - `watermark`：`true`
    - 请求方法：`providerHttpGateway.postJson(...)`（复用连接 metadata 与认证）
    - 返回解析：沿用 `parseImageUrl(...)`，缺失则抛出统一错误。
- 原因：
  - 用户明确要求 `onelinkai` 下所有 seedream 图片模型统一走 `volc` 路径与指定请求格式。
- 实现要点：
  - 保持 `count` 语义不变（按现有循环逐次请求），避免改动前端参数协议。
  - 错误处理沿用 `ProviderGatewayException -> BizException(mapProviderStatus, message)` 既有模式。

### 4) 回归与新增测试
- 文件：`aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
- 修改：
  - 新增用例：OneLink seedream 图片模型会走 `/volc/api/v3/images/generations`，并断言 payload 含上述固定字段。
  - 新增用例：非 seedream 的 `onelinkai` 图片模型不走该分支（保持现有路径）。
- 原因：
  - 当前 OneLink 视频分流已有测试覆盖，图片分流需同等级保护，防止后续回归改坏路由。
- 实现要点：
  - 采用现有 `invokePrivate` + Mockito `argThat` 风格，保持测试风格一致。

## Assumptions & Decisions
- 已确认：仅改 Java 后端，不改 Go 后端。
- 已确认：`onelinkai` 下所有 `seedream*` 图片模型统一使用 `/volc/api/v3/images/generations`。
- 已确认：3 个新模型要同时出现在快捷模式与服务商中心参考模型中。
- 设计决策：不全局替换 `onelinkai` 图片路径；仅在 `GenerationServiceImpl` 基于模型名分流，降低对 `wanx`/Kling 等现有模型风险。
- 设计决策：先按固定参数下发（`2K`、`watermark=true` 等），不新增前端入参，保证改动最小且与给定请求示例一致。

## Verification Steps
- 单元测试：
  - 运行 `GenerationServiceImplViduTest`，确认新增 seedream 分流用例通过。
- 接口层验证（开发环境）：
  - `GET /api/v1/preset-models`：可见 3 个新模型且 capability 含 `image`。
  - `GET /api/v1/provider-catalog`：`onelinkai.staticModels` 包含 3 个新模型。
  - 通过快捷模式创建模型后，`GET /api/v1/models/image` 能返回对应模型。
- 功能联调：
  - 在文生图入口选择 `doubao-seedream-4.0/4.5/5.0-lite` 发起生成，后端应调用 `POST /volc/api/v3/images/generations`，并能回填图片 URL。
  - 剧本数字资产页（依赖 image capability 模型选择）可选中并成功调用上述模型。
