# onelinkai 视频模型认证头从 Token 改为 Bearer 计划

## Summary
- 目标：将 `onelinkai` 视频模型链路中的认证头从 `Authorization: Token {apiKey}` 统一为 `Authorization: Bearer {apiKey}`。
- 成功标准：`onelinkai` 视频请求不再发送 `Token` 前缀；相关单元测试更新并通过；不影响非 onelinkai 提供商既有认证行为。

## Current State Analysis
- 认证头集中在 `aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java` 的 `applyHeaders` 方法内按 `AuthMode` 分支写入。
- `AuthMode.TOKEN` 当前固定写入 `Authorization: Token ...`，属于全局行为，不区分域名/连接来源。
- `onelinkai` 视频主链路在 `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java` 中，`onelinkai` 的 Vidu 视频会路由到 `vidu_onelink`（当前定义是 `AuthMode.BEARER`）。
- 现有测试 `aigc-server/src/test/java/com/example/aigc/service/ProviderHttpGatewayTokenAuthTest.java` 明确覆盖了：
  - `vidu` + `AuthMode.TOKEN` 期望 `Token ...`
  - `vidu_onelink` + `AuthMode.BEARER` 期望 `Bearer ...`
- `ProviderCatalog` 中 `AuthMode.TOKEN` 的语义注释仍写为 Vidu 的 `Token` 模式，位于 `aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`。

## Proposed Changes

### 1) 精准改造 onelinkai 视频链路的认证判定（核心逻辑）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java`
- 变更内容：
  - 调整 `applyHeaders` 的签名或其调用参数，使其可识别当前请求目标（如 `baseUrl` 或解析后的 host）。
  - 在 `AuthMode.TOKEN` 分支增加 onelink 域名判定（`api.onelinkai.cloud` / `api.onelinkai.cloud`）：
    - 命中 onelink 域名时写入 `Authorization: Bearer {apiKey}`
    - 其他场景维持 `Authorization: Token {apiKey}`
- 目的：
  - 满足“onelinkai 视频模型改 Bearer”的要求。
  - 避免直接把全部 `TOKEN` 供应商改成 `BEARER` 导致潜在兼容性回归。

### 2) 同步/补强认证模式注释与可读性
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
- 变更内容：
  - 更新 `AuthMode.TOKEN` 注释，说明存在 onelink 视频链路使用 `Bearer` 的兼容分支（由网关层按目标域名处理）。
- 目的：
  - 防止后续维护误判“TOKEN 永远等于 Token 前缀”。

### 3) 更新并补充单测，锁定行为
- 文件：`aigc-server/src/test/java/com/example/aigc/service/ProviderHttpGatewayTokenAuthTest.java`
- 变更内容：
  - 将当前 `vidu` + onelink URL 的断言改为 `Bearer secret-key`。
  - 保留/新增一个“非 onelink 且 `AuthMode.TOKEN` 仍是 `Token secret-key`”的用例，确保兼容。
  - 保留 `vidu_onelink` 的 `Bearer` 断言。
- 目的：
  - 明确覆盖“onelink 改 Bearer + 非 onelink 保持 Token”双侧行为。

## Assumptions & Decisions
- 决策：只改 onelink 相关视频链路，不做 `AuthMode.TOKEN -> AuthMode.BEARER` 的全局替换。
- 假设：onelinkai 视频请求目标域名可通过网关构建请求时稳定获取并判定。
- 决策：优先在 `ProviderHttpGateway` 层集中处理，避免在各业务 service 分散拼接认证头。

## Verification Steps
- 运行针对性单测：
  - `ProviderHttpGatewayTokenAuthTest`（重点验证认证头前缀切换逻辑）
- 可选回归：
  - 与视频提交/查询相关的 `GenerationServiceImplViduTest`（确认未破坏 onelink 视频流程）
- 代码检查：
  - 全局搜索 `Authorization: Token`，确认 onelink 视频链路不再命中 `Token` 前缀。

