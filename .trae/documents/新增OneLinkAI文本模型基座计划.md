# 新增 OneLinkAI 文本模型基座计划

## Summary
- 目标：将 29 个新文本模型纳入 OneLinkAI 模型基座，使用户在配置 OneLinkAI API 后，可在“快捷配置/服务商目录提示”等入口直接选择并调用。
- 范围：按确认结果仅覆盖 `aigc-server`（真实接口链路）与 `aigc-site-react`（Mock 数据），不改 `aigc-server-go`。
- 接口协议：这些模型统一走 OneLinkAI OpenAI 兼容文本接口（`/v1/chat/completions`）。

## Current State Analysis
- 后端 OneLinkAI 服务商静态模型来源于 `aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java` 的 `onelinkai` `staticModels`。
- 后端快捷配置预置模型来源于 `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`；`/api/v1/preset-models` 直接暴露该注册表。
- 前端快捷配置在真实环境读取 `/api/v1/preset-models`，在 Mock 环境读取 `aigc-site-react/src/api/index.ts` 中 `MOCK_PRESET_MODELS`。
- 前端“添加服务商”里的 OneLinkAI 参考模型 ID 来源于 `aigc-site-react/src/api/index.ts` 中 `MOCK_PROVIDER_CATALOG`（仅 Mock 生效）。

## Proposed Changes

### 1) 后端服务商目录补全 OneLinkAI 文本模型
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
- 修改内容：
- 在 `onelinkai` 的 `staticModels` 中追加以下模型 ID（按用户给定原样保留大小写）：
  - `claude-opus-4-6`
  - `claude-opus-4-7`
  - `claude-sonnet-4-6`
  - `deepseek-v4-flash`
  - `deepseek-v4-pro`
  - `doubao-seed-2.0-code-preview-260215`
  - `doubao-seed-2.0-lite-260215`
  - `doubao-seed-2.0-mini-260215`
  - `doubao-seed-2.0-pro-260215`
  - `gemini-3-flash-preview`
  - `gemini-3.1-pro-preview`
  - `glm-4.7-flashx`
  - `glm-5`
  - `glm-5-turbo`
  - `GLM-5.1`
  - `gpt-5.2-codex`
  - `gpt-5.3-codex`
  - `gpt-5.4`
  - `kimi-k2.5`
  - `kimi-k2.6`
  - `minimax-m2.5`
  - `minimax-m2.7`
  - `qwen3.5-35b-a3b`
  - `qwen3.5-397b-a17b`
  - `qwen3.5-flash`
  - `qwen3.5-plus`
  - `qwen3.6-plus`
  - `step-3.5-flash`
- 说明：不调整 `apiFormat/chatPath/modelsPath`，继续沿用 OneLinkAI 现有 OpenAI 兼容配置。

### 2) 后端快捷配置预置模型补全
- 文件：`aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
- 修改内容：
- 新增 29 条 `onelinkai` + `text` 能力预置项，`baseUrl` 统一 `https://api.onelinkai.cloud`。
- `displayName` 采用统一规则：`OneLinkAI + 空格 + 模型ID`（例如 `OneLinkAI gpt-5.4`）。
- 实现细节：
  - 采用小范围抽取辅助方法（如 `onelinkText(modelId)`）减少重复，避免手工拼写错误。
  - 保留现有图片/视频模型预置不变。

### 3) 前端 Mock 预置模型补全（离线/无后端场景）
- 文件：`aigc-site-react/src/api/index.ts`
- 修改内容：
- 在 `MOCK_PRESET_MODELS.models` 里追加同一批 29 条 OneLinkAI 文本模型，字段映射与后端一致：
  - `provider: 'onelinkai'`
  - `modelName: <模型ID>`
  - `baseUrl: 'https://api.onelinkai.cloud'`
  - `displayName: 'OneLinkAI <模型ID>'`
  - `capabilities: ['text']`

### 4) 前端 Mock 服务商目录参考模型补全
- 文件：`aigc-site-react/src/api/index.ts`
- 修改内容：
- 在 `MOCK_PROVIDER_CATALOG` 的 `onelinkai.staticModels` 中追加同一批模型 ID，用于“添加服务商”弹窗中的参考模型展示。

## Assumptions & Decisions
- 决策：覆盖 `Java + 前端Mock`，不做 Go 端同步。
- 决策：展示名采用自动规则 `OneLinkAI <模型ID>`，不单独维护中文别名。
- 决策：模型 ID 严格按给定列表入库，包含 `GLM-5.1` 的大小写写法。
- 假设：新增模型均属于文本能力，统一标记 `capabilities = ['text']`。
- 假设：OneLinkAI 文本调用路径维持 `POST /v1/chat/completions`，无需新增 provider 或 endpoint。

## Verification Steps
- 后端验证：
  - 启动 `aigc-server` 后请求 `GET /api/v1/provider-catalog`，确认 `onelinkai.staticModels` 含新增 ID。
  - 请求 `GET /api/v1/preset-models`，确认新增模型出现在 `models` 且 `capabilities` 为 `text`。
  - 调用 `POST /api/v1/connections/quick`（`provider=onelinkai` + 任一新增 `modelName`）应成功创建连接与模型。
- 前端验证：
  - 在无后端/Mock 模式打开“快捷配置”，选择 `onelinkai` 可看到新增模型并可提交。
  - 在“添加服务商”弹窗选择 `onelinkai`，参考模型 chips 可反映新增清单（展示前 6 个 + 省略号逻辑保持不变）。
- 回归关注：
  - 现有 onelinkai 图片/视频模型不丢失。
  - 现有其他 provider 的预置模型不受影响。
