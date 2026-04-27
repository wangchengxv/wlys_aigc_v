# Summary
- 目标：评估并规划 Spring AI 在当前 `aigc-server` 工程中的可落地点，优先降低多供应商调用复杂度、统一文本能力调用路径、提升可观测性与可测试性。
- 结论：最适合优先接入 Spring AI 的是“文本对话链路”（路由代理、剧本工作流、反推提示词）；图片可部分引入；视频链路（Vidu/Kling/Moark/豆包任务轮询）先保留现有实现。
- 交付方式：采用“分阶段迁移 + 双通道回退（Spring AI / 现网网关）”，避免一次性重写 `ProviderHttpGateway` 与视频编排逻辑。

# Current State Analysis
- 依赖层面尚未引入 Spring AI：`aigc-server/pom.xml` 当前无 `org.springframework.ai` 相关依赖。
- 统一网关集中在 `aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java`，大量使用 `HttpClient + Map<String, Object>` 进行请求/响应编排与格式转换。
- 文本能力调用分散在多个核心服务：
  - `aigc-server/src/main/java/com/example/aigc/service/RouterProxyService.java`
  - `aigc-server/src/main/java/com/example/aigc/service/ScriptWorkflowService.java`
  - `aigc-server/src/main/java/com/example/aigc/service/impl/ReversePromptServiceImpl.java`
  - `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 供应商定义集中在 `aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`，覆盖 OpenAI/Anthropic/Azure/Bedrock/Vertex/OneLink/Ark/Vidu/Kling/Moark，多数靠路径与鉴权规则手工拼装。
- Bedrock 与 Vertex 已有专门适配：
  - `aigc-server/src/main/java/com/example/aigc/service/BedrockGatewayService.java`
  - `aigc-server/src/main/java/com/example/aigc/service/VertexAiGatewayService.java`
- 视频链路高度定制（任务提交、轮询、多家厂商差异、multipart、首帧规则），关键逻辑在：
  - `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - `aigc-server/src/main/java/com/example/aigc/service/ScriptProductionOrchestrator.java`
- 现有测试以网关/视频规则为主，文本调用尚有结构化提升空间：
  - `aigc-server/src/test/java/com/example/aigc/service/ProviderHttpGatewayTokenAuthTest.java`
  - `aigc-server/src/test/java/com/example/aigc/service/ReversePromptServiceImplTest.java`
  - `aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`

# Proposed Changes
## Phase 1（高优先级）：文本链路 Spring AI 化（收益最大，风险可控）
- 文件：`aigc-server/pom.xml`
  - What：引入 Spring AI BOM 与文本相关 starter（OpenAI/Anthropic/Azure/OpenAI 兼容入口），保留现有 SDK 依赖。
  - Why：先覆盖最通用、最重复的文本能力调用，减少自维护协议转换成本。
  - How：采用 BOM 管理版本；先只加聊天所需模块，不引入无用 starter。

- 新增：`aigc-server/src/main/java/com/example/aigc/service/ai/SpringAiClientFactory.java`
  - What：按连接配置（baseUrl/apiKey/provider/apiFormat）动态构建 `ChatClient`/`ChatModel`。
  - Why：当前模型连接是用户配置驱动，不能只靠静态 Bean；需要运行时工厂。
  - How：封装 provider -> Spring AI 模型构造策略，并输出统一调用接口。

- 新增：`aigc-server/src/main/java/com/example/aigc/service/ai/TextGenerationFacade.java`
  - What：定义统一文本调用门面（普通问答、结构化 JSON 输出、流式输出）。
  - Why：把 `Map<String,Object>` 与 provider 特殊分支从业务服务中抽离。
  - How：对外仅暴露领域参数；内部优先走 Spring AI，失败回退 `ProviderHttpGateway.invokeChat/streamChat`。

- 修改：`aigc-server/src/main/java/com/example/aigc/service/RouterProxyService.java`
  - What：文本代理改为优先走 `TextGenerationFacade`。
  - Why：该处是 `/v1/chat/completions` 与 `/v1/messages` 核心入口，统一后收益最大。
  - How：保留现有 `formatConversionService` 与 failover 逻辑，替换底层调用实现而不改接口契约。

- 修改：`aigc-server/src/main/java/com/example/aigc/service/ScriptWorkflowService.java`
  - What：`refine/rewrite/append` 等 `invokeChat` 调用改走 `TextGenerationFacade`。
  - Why：该服务文本调用频繁、错误处理复杂，最适合通过 Spring AI 的消息抽象降复杂度。
  - How：先替换文本路径，不改现有 prompt 模板与结构化解析逻辑。

- 修改：`aigc-server/src/main/java/com/example/aigc/service/impl/ReversePromptServiceImpl.java`
  - What：图像理解+文本输出的聊天调用改走统一文本门面。
  - Why：当前手工解析 OpenAI/Anthropic 响应分支较多，迁移后可减少分支和兼容成本。
  - How：保持输入校验与业务输出 DTO 不变，仅替换底层调用。

## Phase 2（中优先级）：可观测性与重试治理
- 新增：`aigc-server/src/main/java/com/example/aigc/service/ai/AiCallObservationSupport.java`
  - What：统一记录模型调用耗时、成功率、供应商/模型标签、异常分类。
  - Why：当前日志分散在业务服务，难以统一评估模型质量与成本。
  - How：在 `TextGenerationFacade` 入口埋点；与现有 `RouterRequestLog` 并行，避免破坏已有统计。

- 修改：`aigc-server/src/main/java/com/example/aigc/service/RouterProxyService.java`
  - What：保留原 failover 策略，增加按异常类型区分重试/切换策略。
  - Why：401、429、5xx 在切换策略上应该差异化。
  - How：通过统一异常映射层将 Spring AI 异常映射为现有 `ProviderGatewayException`。

## Phase 3（低优先级）：图片能力择机接入，视频维持现状
- 修改：`aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java`
  - What：文本相关方法逐步瘦身，保留图片/视频/multipart/任务轮询能力。
  - Why：视频路径差异化极大，Spring AI 当前并不能直接替代现有任务式视频 API 适配。
  - How：按“文本先迁出、视频后保留”的方向拆分职责，降低单类体积与维护风险。

- 修改：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - What：仅文本生成分支迁移到统一门面；图片和视频暂不强行统一到 Spring AI。
  - Why：避免影响 Vidu/Kling/Moark/豆包视频规则（时长、分辨率、首帧、轮询）稳定性。
  - How：对 `generateTextContent` 做替换，`generateImages/generateVideos` 保持现状。

# Assumptions & Decisions
- 范围聚焦 `aigc-server`，不涉及前端与 Python `ComfyUI` 子项目。
- 保持所有现有 API 路径与响应结构不变（尤其 `/v1/chat/completions`、`/v1/messages`、`/api/v1/script-projects/*`）。
- Spring AI 作为能力增强层，不替换已有业务路由决策（连接优先级、模型匹配、故障切换策略）。
- 视频相关能力（任务提交+轮询+厂商定制参数）继续由现有实现主导，不作为首批迁移目标。
- 必须保留回退通路：当 Spring AI 调用失败时自动回落到 `ProviderHttpGateway`，确保上线可控。

# Verification Steps
- 单元测试：
  - 新增 `TextGenerationFacade` 测试，覆盖 OpenAI/Anthropic 格式、流式与异常映射。
  - 更新 `RouterProxyService` 相关测试，验证 failover 与流式输出行为不变。
  - 保留并通过现有测试：`ProviderHttpGatewayTokenAuthTest`、`ReversePromptServiceImplTest`、`GenerationServiceImplViduTest`。
- 集成测试：
  - 回归 `ScriptProjectWorkflowIntegrationTest`、`ScriptProjectOptimizeIntegrationTest`，确认剧本工作流文本环节无回归。
  - 回归 `/v1/chat/completions` 与 `/v1/messages` 的非流式/流式调用。
- 人工验收：
  - 对比迁移前后同模型下的响应结构、错误码、耗时分布。
  - 验证多供应商切换（至少 OpenAI-compatible + Anthropic + Bedrock/Vertex 任一）可正常回退。
