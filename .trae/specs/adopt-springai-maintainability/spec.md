# Spring Boot + SpringAI 可维护性改造 Spec

## Why
当前后端 AI 能力接入以手工 HTTP 与多处 `Map<String,Object>` 转换为主，文本能力调用重复且分散。通过 Spring Boot + SpringAI 统一文本模型调用入口，可降低协议适配复杂度并提高后续扩展与维护效率。

## What Changes
- 盘点并明确“可优先由 SpringAI 承接”的功能范围（文本对话、剧本工作流文本环节、反推提示词文本环节）。
- 定义统一文本调用层（Facade）与客户端工厂（Factory）规范，支持多供应商动态接入与失败回退。
- 约束路由代理与业务服务的调用边界：业务层不直接处理供应商协议细节。
- 保留视频任务型能力（提交+轮询+厂商定制参数）在现有网关实现，不在首批强迁。
- 建立迁移验收标准（接口兼容、回退机制、测试覆盖、可观测性）。

## Impact
- Affected specs:
  - 模型路由与多供应商接入
  - 剧本工作流文本生成
  - 反推提示词文本解析
  - 通用生成服务文本分支
- Affected code:
  - `aigc-server/pom.xml`
  - `aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java`
  - `aigc-server/src/main/java/com/example/aigc/service/RouterProxyService.java`
  - `aigc-server/src/main/java/com/example/aigc/service/ScriptWorkflowService.java`
  - `aigc-server/src/main/java/com/example/aigc/service/impl/ReversePromptServiceImpl.java`
  - `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - `aigc-server/src/test/java/com/example/aigc/service/*`

## ADDED Requirements
### Requirement: SpringAI 功能可承接范围清单
系统 SHALL 提供可执行的 SpringAI 承接清单，明确哪些现有功能适合优先迁移、哪些功能暂不迁移。

#### Scenario: 输出可迁移功能清单
- **WHEN** 团队评估当前功能的 SpringAI 适配性
- **THEN** 产出“优先迁移（文本）/暂缓迁移（视频任务型）”的分层清单与依据

#### Scenario: 基于仓库现状输出功能映射表与风险依据
- **WHEN** 团队需要将“可迁移结论”落到当前代码
- **THEN** 应提供如下映射与风险依据，作为 Task 1 的交付基线

| 功能域 | 代表代码路径（仓库现状） | 当前实现形态 | SpringAI 映射策略 | 迁移优先级 | 主要风险依据 |
| --- | --- | --- | --- | --- | --- |
| 路由文本代理（OpenAI/Anthropic） | `aigc-server/src/main/java/com/example/aigc/controller/RouterProxyController.java`、`aigc-server/src/main/java/com/example/aigc/service/RouterProxyService.java` | 通过 `ProviderHttpGateway.invokeChat/streamChat` + `FormatConversionService` 做协议转换与失败切换 | 用 `TextGenerationFacade` 承接文本请求构建与调用；保留现有路由选路、鉴权、日志、failover 语义 | 优先迁移 | 现有接口需保持 `/v1/chat/completions`、`/v1/messages` 响应兼容；流式链路含降级逻辑，改造不当会影响 SSE 行为 |
| 剧本工作流文本环节（完善/改写/续写/优化） | `aigc-server/src/main/java/com/example/aigc/service/ScriptWorkflowService.java` | 多处 `Map<String, Object>` 组装 payload、解析内容与 JSON 回填 | 统一改为走 `TextGenerationFacade`；保留业务侧结构化校验与 fallback 逻辑 | 优先迁移 | 工作流对 JSON 结构完整性高度敏感，若模型返回格式波动会放大解析失败风险 |
| 反推提示词（图文输入，文本输出） | `aigc-server/src/main/java/com/example/aigc/service/impl/ReversePromptServiceImpl.java`、`aigc-server/src/test/java/com/example/aigc/service/ReversePromptServiceImplTest.java` | 服务内做图片输入校验、消息拼装、文本抽取与 JSON 解析 | 文本调用入口迁入 `TextGenerationFacade`；图片输入合法性校验与业务响应解析继续留在领域服务 | 优先迁移（分阶段） | 该链路含 `image_url` 多模态消息与严格 JSON 输出约束，需验证 SpringAI 适配后字段一致性 |
| 通用生成文本分支 | `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java` | 文本分支直接构造请求并解析响应，和图片/视频逻辑耦合在同服务 | 仅抽离文本调用到统一门面；图片/视频分支暂不动 | 优先迁移（仅文本） | 同一服务承载多能力，重构边界不清会引入回归；需控制为“文本最小改动” |
| 图片生成（多厂商特化） | `aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java`、`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java` | 依赖 `imageGenerationPath` 和厂商特定 payload（如 Kling/Seedream 分支） | 首批不纳入 SpringAI，继续走现有网关 | 暂缓迁移 | 已有大量厂商参数映射和分支判断，短期替换收益低且回归面大 |
| 视频任务型能力（提交+轮询） | `aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java`、`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`、`aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java` | 提交任务、轮询状态、解析多种返回结构（`task_id/id`、`video_url/content.videoUrl`） | 保留现有网关与轮询编排，不进入首批 SpringAI 迁移 | 暂缓迁移 | 任务型接口和厂商状态机差异显著，且已有较多行为测试约束，强迁风险高 |

**风险依据（仓库证据）**
- 依赖基线风险：`aigc-server/pom.xml` 当前无 `spring-ai-*` 依赖，迁移前需先补齐 BOM 与 starter，并评估与现有 Spring Boot 版本兼容性。
- 协议异构风险：`ProviderCatalog` 同时维护多种 `AuthMode` 与 `GatewayKind`（如 `OPENAI_COMPAT`、`ANTHROPIC`、`AZURE_OPENAI`、`BEDROCK`、`VERTEX`），统一抽象需避免丢失厂商特性。
- 结构化解析风险：`ScriptWorkflowService`、`ReversePromptServiceImpl`、`GenerationServiceImpl` 均大量依赖 `Map<String,Object>` 与手工字段提取，响应 schema 稍有变化即可能触发业务异常。
- 流式兼容风险：`RouterProxyService.proxyChatStream` 存在“流式失败后回退非流式再封装 SSE”的现状，迁移后必须保留相同行为。
- 视频链路回归风险：`GenerationServiceImplViduTest` 覆盖了大量参数校验、路径选择与轮询解析场景，说明视频能力已形成高耦合特化实现，不宜纳入首批 SpringAI。

### Requirement: 统一文本调用门面
系统 SHALL 提供统一文本调用门面，封装请求构建、模型调用、异常归一化与回退策略。

#### Scenario: 业务服务调用文本能力
- **WHEN** 业务服务需要调用文本模型
- **THEN** 通过统一门面完成调用，而非直接拼装供应商 HTTP 请求

#### Scenario: `TextGenerationFacade` 契约定义
- **WHEN** 团队实现统一文本调用门面
- **THEN** 门面契约应至少包含以下输入、输出与异常语义，确保路由代理与业务服务对齐

| 契约维度 | 约束 |
| --- | --- |
| 输入对象（`TextGenerationRequest`） | SHALL 包含：`providerCode`、`modelCode`、`messages`、`stream`、`temperature/topP/maxTokens`、`responseFormat`、`requestId`、`timeoutMs`、`extraMetadata`。其中 `messages` 使用统一角色语义（`system/user/assistant/tool`）。 |
| 调用上下文（`TextGenerationContext`） | SHALL 包含：`scene`（如 `router_proxy/script_workflow/reverse_prompt`）、`tenantId`、`userId`、`traceId`、`fallbackEnabled`，用于路由、审计与回退判定。 |
| 输出对象（`TextGenerationResult`） | SHALL 提供：`text`、`finishReason`、`usage`（`promptTokens/completionTokens/totalTokens`）、`provider/model`、`rawResponseRef`（脱敏引用）与 `fallbackOccurred` 标记。 |
| 流式输出（`TextGenerationStreamChunk`） | SHALL 提供增量 `delta`、`isFinal`、`finishReason`，并可映射到现有 SSE 输出格式，保证 `RouterProxyService` 对外行为不变。 |
| 异常基类 | SHALL 归一为 `TextGenerationException`，并按语义细分：`AuthException`、`RateLimitException`、`Upstream5xxException`、`TimeoutException`、`ProtocolMismatchException`、`InvalidRequestException`。 |
| 错误码与可恢复标记 | SHALL 在异常中包含 `errorCode`、`httpStatus`、`retryable`、`fallbackAllowed`，用于统一告警、重试与回退决策。 |
| 兼容性约束 | SHALL 不向业务层暴露供应商 SDK/HTTP 细节；现有业务层仅依赖上述 DTO 与异常语义。 |

### Requirement: 动态客户端构建能力
系统 SHALL 基于连接配置动态构建 SpringAI 客户端，以支持多供应商与多模型。

#### Scenario: 路由命中不同连接
- **WHEN** 路由切换到新的连接配置
- **THEN** 系统可按连接元数据动态构建对应模型客户端并完成调用

#### Scenario: `SpringAiClientFactory` 契约定义
- **WHEN** 工厂根据连接配置创建 SpringAI 客户端
- **THEN** 必须遵循统一构建规则，确保多供应商能力一致可控

| 契约维度 | 约束 |
| --- | --- |
| 工厂输入（`ClientBuildSpec`） | SHALL 至少包含：`connectionId`、`providerCode`、`baseUrl`、`modelCode`、`apiKeyRef`、`authMode`、`timeoutMs`、`proxy`、`headers`、`metadata`。 |
| 鉴权解析 | SHALL 按 `authMode` 规范化为 SpringAI 可识别鉴权头（如 `Authorization: Bearer`、厂商自定义 Header）；密钥仅从安全配置读取，不落盘到业务日志。 |
| 模型映射 | SHALL 支持 `providerModelAlias -> providerRealModel` 映射；当路由传入别名时由工厂统一解析。 |
| 客户端缓存策略 | SHALL 使用 `(connectionId, modelCode, authFingerprint)` 作为缓存键；连接配置变更后触发失效重建。 |
| 元数据透传 | SHALL 支持透传 `metadata` 到客户端扩展参数（例如 `api-version`、`deployment`、`region`），并提供白名单校验。 |
| 构建失败语义 | SHALL 统一抛出 `ClientBuildException`，并包含 `providerCode`、`connectionId`、`reason`，供门面执行回退。 |
| 可观测性 | SHALL 记录构建耗时、命中缓存比例、构建失败率三类指标，用于迁移期健康度评估。 |

### Requirement: 调用失败回退保障
系统 SHALL 在 SpringAI 调用失败时回退到既有网关路径，保证线上可用性与平滑迁移。

#### Scenario: SpringAI 调用异常
- **WHEN** 出现鉴权失败、上游异常或协议不兼容
- **THEN** 系统触发回退并保持对外接口结构与错误语义兼容

#### Scenario: SpringAI 优先 + 网关回退执行流程
- **WHEN** 任一文本调用进入 `TextGenerationFacade`
- **THEN** 执行以下流程并满足兼容性约束

1. 门面先依据 `TextGenerationRequest + TextGenerationContext` 进行参数校验与标准化；校验失败直接返回 `InvalidRequestException`，不进入回退。  
2. 门面调用 `SpringAiClientFactory` 获取/构建客户端，构建失败且 `fallbackEnabled=true` 时进入网关回退；否则抛出 `ClientBuildException`。  
3. SpringAI 调用成功时，统一映射为 `TextGenerationResult` 或流式 chunk，并透出 `fallbackOccurred=false`。  
4. SpringAI 调用出现 `AuthException/RateLimitException/Upstream5xxException/TimeoutException/ProtocolMismatchException` 且异常标记 `fallbackAllowed=true` 时，进入既有 `ProviderHttpGateway` 路径。  
5. 回退成功时，输出结构与当前接口保持一致，并在结果中标记 `fallbackOccurred=true`，同时上报回退原因、供应商、模型、耗时。  
6. 回退失败时，输出“主调用异常 + 回退异常”的归一化错误语义，保持现有 HTTP 状态码与错误体兼容。  
7. 对流式请求，若主调用中途失败，需保持现有“降级到非流式再封装 SSE”的行为一致性，不改变客户端消费方式。  

#### Scenario: 回退触发与禁止矩阵
- **WHEN** 系统判断是否允许回退
- **THEN** 应按以下规则执行，避免无意义重试

| 异常类型 | 默认是否回退 | 说明 |
| --- | --- | --- |
| `InvalidRequestException` | 否 | 入参不合法，回退无法修复，应直接返回 4xx。 |
| `AuthException` | 是 | 允许切换到既有网关重试一次；若同凭证必然失败则可配置禁用。 |
| `RateLimitException` | 是 | 允许回退，避免单通道路由限流导致整体不可用。 |
| `Upstream5xxException` | 是 | 上游不稳定时回退可提升成功率。 |
| `TimeoutException` | 是 | 主链路超时时允许回退，但需受总超时预算控制。 |
| `ProtocolMismatchException` | 是 | 适配层字段不兼容时回退，保障外部接口稳定。 |
| `ClientBuildException` | 是 | 工厂构建失败可直接使用既有网关。 |

### Requirement: 验证与回归验收标准
系统 SHALL 提供可执行、可度量的迁移验收标准，覆盖接口兼容、测试范围与可观测性三类基线。

#### Scenario: 接口兼容性验收标准
- **WHEN** 团队执行 SpringAI 迁移验收
- **THEN** 以下接口兼容项必须全部通过，方可进入灰度放量或全量切换

| 验收维度 | 通过标准 |
| --- | --- |
| 非流式响应结构兼容 | `/v1/chat/completions` 与 `/v1/messages` 的响应 JSON 关键字段（如 `id/object/model/choices/usage`）与现网契约一致；允许新增非破坏性字段，不允许删除或重命名既有字段。 |
| 错误码与错误体兼容 | 4xx/5xx 分类与核心错误码语义保持一致；错误体中的可读 message、错误类型与可重试语义不弱于现网。 |
| 流式行为兼容 | 流式首包时延、chunk 终止标记与 SSE 事件序列保持既有消费语义；主链路中断时仍保持“降级非流式后再封装 SSE”的兼容行为。 |
| 回退结果兼容 | 发生回退时，对外响应结构、HTTP 状态码、业务错误语义与非回退路径保持一致，不引入新增前端分支处理。 |
| 上下游契约兼容 | `RouterProxyService`、`ScriptWorkflowService`、`ReversePromptServiceImpl`、`GenerationServiceImpl` 的调用入参与出参结构对业务层保持不变。 |

#### Scenario: 测试范围验收标准
- **WHEN** 团队完成迁移代码并准备提交验收
- **THEN** 必须覆盖以下测试层级与关键回归用例，且阻断级用例全部通过

| 测试层级 | 必选范围 | 最低通过标准 |
| --- | --- | --- |
| 单元测试 | `TextGenerationFacade` 参数校验、异常归一化、回退判定；`SpringAiClientFactory` 构建、缓存与失效策略；关键解析器（结构化输出映射） | 新增与改造相关单测全部通过，核心分支（成功/失败/回退）均有断言。 |
| 集成测试 | SpringAI 主路径调用、网关回退路径调用、流式降级链路、配置切换后客户端重建 | 关键集成用例全部通过，且无阻断性回归。 |
| 回归测试 | 路由代理（流式/非流式）、剧本工作流文本环节、反推提示词、通用生成文本分支；保留视频能力不回归受损 | 既有关键回归用例持续通过，迁移范围外能力（图片/视频）无行为退化。 |
| 兼容性对比测试 | 新旧链路同输入对比（结构、错误语义、耗时区间） | 对比结果满足接口兼容约束，差异项具备可解释记录并经评审批准。 |

#### Scenario: 可观测性验收标准
- **WHEN** 迁移链路进入联调、灰度或生产观测阶段
- **THEN** 系统必须提供以下指标、日志与告警基线，支持分钟级定位与回切决策

| 类别 | 验收项 | 通过标准 |
| --- | --- | --- |
| 指标 | 成功率 | 提供按 `scene/provider/model` 维度成功率指标；灰度期成功率不低于现网基线，异常波动可在监控面板中被识别。 |
| 指标 | 耗时 | 提供 P50/P95/P99 与超时率指标，区分主链路与回退链路；可按租户或路由维度下钻。 |
| 指标 | 回退率 | 提供 `fallbackOccurred` 比例与按异常类型拆分的回退原因分布；支持按版本与开关状态对比。 |
| 日志 | 结构化链路日志 | 每次调用记录 `traceId/requestId/scene/provider/model/fallbackOccurred/errorCode`，敏感信息脱敏，支持链路串联。 |
| 告警 | 阈值与响应 | 对成功率下降、P95 升高、回退率激增设置阈值告警，并具备“告警触发 -> 回切开关”操作指引。 |

## MODIFIED Requirements
### Requirement: 文本能力接入方式
现有文本能力接入 SHALL 从“业务服务直接依赖底层网关细节”调整为“业务服务依赖统一文本门面，网关仅作为兼容回退能力”。

#### Scenario: 路由代理处理聊天请求
- **WHEN** `/v1/chat/completions` 或 `/v1/messages` 收到请求
- **THEN** 优先走 SpringAI 门面执行，并保留现有 failover 与日志语义

### Requirement: 剧本工作流文本调用路径
剧本工作流中的文本生成环节 SHALL 复用统一文本门面，以减少重复解析与分支逻辑。

#### Scenario: 剧本完善/改写/续写
- **WHEN** 工作流触发文本生成
- **THEN** 使用统一门面调用并保持原有输出结构与业务校验不变

### Requirement: 关键链路分批迁移方案
系统 SHALL 为三条关键文本链路提供可执行的分批迁移方案，遵循“灰度切流、可回退、接口不变”的实施原则。

#### Scenario: RouterProxy 链路迁移方案（`RouterProxyService`）
- **WHEN** 团队实施路由代理文本链路迁移
- **THEN** 采用以下顺序执行：  
1. 在 `RouterProxyService` 引入 `TextGenerationFacade` 适配层，仅替换模型调用实现，不调整 Controller 入参与出参结构。  
2. 先完成非流式 `/v1/chat/completions` 主路径迁移，验证错误码、usage 字段与失败切换语义一致。  
3. 再迁移流式路径，保留“流式失败后降级非流式并封装 SSE”的既有行为。  
4. 通过开关按租户/路由灰度放量，监控 `fallbackOccurred`、成功率与 P95 时延后再全量。  
5. 任一阶段出现兼容性回归时，开关回切到 `ProviderHttpGateway` 全量路径，保证分钟级止损。  

#### Scenario: ScriptWorkflow 链路迁移方案（`ScriptWorkflowService`）
- **WHEN** 团队实施剧本工作流文本链路迁移
- **THEN** 采用以下顺序执行：  
1. 按能力点拆分迁移批次：`完善 -> 改写 -> 续写/优化`，每批次只替换文本调用入口。  
2. 保留原有 prompt 组织、JSON schema 校验与 fallback 逻辑，避免一次性重构业务编排。  
3. 新增统一结果解析器，将 `TextGenerationResult.text` 映射回现有工作流结构字段，确保前端消费不变。  
4. 对每个批次执行回归：结构化字段完整率、解析失败率、重试与回退比例。  
5. 批次验收通过后再推进下一批，任何批次失败可独立回滚，不影响已稳定批次。  

#### Scenario: ReversePrompt + Generation 文本分支迁移方案（`ReversePromptServiceImpl`、`GenerationServiceImpl`）
- **WHEN** 团队实施反推提示词与通用生成文本分支迁移
- **THEN** 采用以下顺序执行：  
1. 先迁移 `ReversePromptServiceImpl` 的文本调用，保留图片输入合法性校验、JSON 解析与业务错误语义。  
2. 完成 `ReversePrompt` 稳定后，再迁移 `GenerationServiceImpl` 的文本分支，仅抽离文本路径，图片/视频分支完全保持现状。  
3. 为 `GenerationServiceImpl` 增加明确分流条件，确保文本请求走门面、非文本请求继续走既有网关。  
4. 验证两条链路在异常场景下的回退一致性（鉴权失败、超时、协议不匹配），并校验响应结构兼容。  
5. 最终通过特性开关实现“双链路独立启停”，确保任一链路异常时可单独回切。  

## REMOVED Requirements
### Requirement: 业务层手工维护供应商协议细节
**Reason**: 协议细节分散在多个服务会增加维护成本与变更风险。  
**Migration**: 将协议差异收敛到统一文本门面与客户端工厂，业务层仅传递领域参数。
