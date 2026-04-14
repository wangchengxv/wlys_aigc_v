# aigc-server-go 剩余工作清单

本文档对照 [aigc-server](../aigc-server) 与前端 [aigc-site-react/src/api/index.ts](../aigc-site-react/src/api/index.ts)，列出 **Go 后端尚未完成或仅部分完成** 的事项，便于按优先级推进。

---

## 已具备的基础（便于对照）

- 配置、MySQL DSN、`ApiResponse`、鉴权（Bearer / `x-aigc-token`、`x-user-id`）、`/api/**` CORS、Recovery  
- GORM 模型与 `script_project` 聚合保存策略（删子表再插入）  
- `GenerationService` 主路径（text/image/video/both）、方舟图片/视频、工作台落盘、本地文件服务  
- Provider 目录、`ProviderHttpGateway`（HTTP/Vertex/Moark multipart）、Comfy `/api/comfy/**` 反向代理  
- 部分只读接口：`GET /connections`、`GET /models`、预设/目录占位、`/api/v1/health`、生成/历史/任务  
- 并行验收：`RUNBOOK.md`、`scripts/parity/compare.sh`、`testdata/golden/health.json`  
- Prompt 资源：`internal/prompts/embed` + [`internal/prompts/prompts.go`](internal/prompts/prompts.go)（用户已删除重复的 `internal/prompt/prompt.go`，以 `prompts` 包为准）

---

## P2：连接与模型配置（完整 CRUD 与探测）

- [ ] `POST/PUT/DELETE /api/v1/connections`、`POST /connections/quick`、`GET /connections/:id`、`POST /connections/:id/test` — 对齐 [`ConnectionConfigController`](../aigc-server/src/main/java/com/example/aigc/controller/ConnectionConfigController.java) + [`ConnectionConfigService`](../aigc-server/src/main/java/com/example/aigc/service/ConnectionConfigService.java)  
- [ ] `POST/PUT/DELETE /api/v1/models`、`POST /models/batch-import`、`POST /models/:id/probe`、`GET /models/:id` — 对齐 [`ModelConfigController`](../aigc-server/src/main/java/com/example/aigc/controller/ModelConfigController.java)  
- [ ] `GET /models/image`、`GET /models/video`：按 Java 逻辑合并 **已启用模型 + Router 路由顺序 + Ark 回退**（当前为 Ark 静态占位）  
- [ ] `GET /preset-models`：接入 [`PresetModelRegistry`](../aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java) 等价数据  
- [ ] `GET /provider-catalog`：返回与 Java [`ProviderCatalog`](../aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java) 一致的字段（能力、网关类型等），`oauth-notes` 对齐  
- [ ] 连接 metadata：[`ConnectionMetadataHelper`](../aigc-server/src/main/java/com/example/aigc/service/ConnectionMetadataHelper.java) 的 `normalizeIncoming` / `decryptForUse` / `__ENC__` 全路径  

---

## P3：生成任务与文件

- [ ] `GET /api/v1/files/:fileId`、`GET .../download` — [`FileAssetController`](../aigc-server/src/main/java/com/example/aigc/controller/FileAssetController.java) + 与 `LocalAssetFileService` 一致的 Content-Type / 范围请求（若 Java 有）  
- [ ] 历史/任务分页与 `mode` 枚举校验与 Java [`JpaGenerationTaskRepository`](../aigc-server/src/main/java/com/example/aigc/repository/jpa/JpaGenerationTaskRepository.java) 行为一致（含 `owner_id` 过滤边界情况）  
- [ ] 全局异常：校验失败 `400` 文案、[`MethodArgumentNotValidException`](../aigc-server/src/main/java/com/example/aigc/exception/GlobalExceptionHandler.java) 与 Gin binding 对齐  

---

## P4：剧本工程（ScriptProject + Docx + 脚本读写）

- [ ] `GET/POST /script-projects`、`POST /upload`、`GET/DELETE /:id`、`POST /:id/restore`  
- [ ] `POST refine`、`refine-with-brief`、`GET/PUT script`、`POST import`、`revisions` 与 `restore`  
- [ ] `optimize/scenes|characters|props`、`append/preview`、`rewrite/preview|apply`  
- [ ] [`ScriptDocxService`](../aigc-server/src/main/java/com/example/aigc/service/ScriptDocxService.java) 等价（.docx 解析/生成）  
- [ ] `GET/PUT .../model-settings`、`GET/PUT .../prompt-template-overrides`  
- [ ] 实现或接入 [`PromptTemplateService`](../aigc-server/src/main/java/com/example/aigc/service/PromptTemplateService.java)（`internal/prompts` 已嵌入模板）  

---

## P5：工作流大项（ScriptWorkflow + 视频编排）

- [ ] 按子域拆分迁移 [`ScriptWorkflowService`](../aigc-server/src/main/java/com/example/aigc/service/ScriptWorkflowService.java)（~3300 行）：资产抽取/更新、关键帧、视觉提示、分镜、rewrite、rollback、群像/三视图等  
- [ ] [`AiCapabilityRoutingService`](../aigc-server/src/main/java/com/example/aigc/service/AiCapabilityRoutingService.java) 完整解析（text/image/video 与 system fallback）  
- [ ] [`ScriptProductionOrchestrator`](../aigc-server/src/main/java/com/example/aigc/service/ScriptProductionOrchestrator.java)：视频任务队列、并发上限、重试、与 `PipelineVideoProperties` 一致  
- [ ] 控制器对齐：[`ScriptAssetController`](../aigc-server/src/main/java/com/example/aigc/controller/ScriptAssetController.java)、[`KeyframeController`](../aigc-server/src/main/java/com/example/aigc/controller/KeyframeController.java)、[`VisualPromptController`](../aigc-server/src/main/java/com/example/aigc/controller/VisualPromptController.java)、[`VideoPipelineController`](../aigc-server/src/main/java/com/example/aigc/controller/VideoPipelineController.java)  

---

## P6：Canvas 与资产历史

- [ ] [`CanvasGraphController`](../aigc-server/src/main/java/com/example/aigc/controller/CanvasGraphController.java) CRUD + `owner_id` 隔离  
- [ ] [`AssetHistoryController`](../aigc-server/src/main/java/com/example/aigc/controller/AssetHistoryController.java) list + restore  
- [ ] [`AssetHistoryService`](../aigc-server/src/main/java/com/example/aigc/service/AssetHistoryService.java) + [`PromptVersionService`](../aigc-server/src/main/java/com/example/aigc/service/PromptVersionService.java) 与 JSON 列格式一致  

---

## P7：Router 管理 + OpenAI 兼容代理

- [ ] [`RouterAdminController`](../aigc-server/src/main/java/com/example/aigc/controller/RouterAdminController.java)：keys CRUD、routing、logs、stats、config import/export  
- [ ] [`RouterProxyController`](../aigc-server/src/main/java/com/example/aigc/controller/RouterProxyController.java)：**无 `/api` 前缀** 的 `/v1/chat/completions`（含 **SSE 流式**）、`/v1/messages`、`/v1/models` — 与 [`RouterProxyService`](../aigc-server/src/main/java/com/example/aigc/service/RouterProxyService.java) 一致  
- [ ] Router 侧鉴权（`x-api-key` / Router API Key）与日志落库  
- [ ] CORS：若前端直连 Router 路径，评估是否需与 Java 行为一致的额外头（当前仅 `/api/**` 配 CORS）  

---

## P8：外部网关与实现缺口

- [ ] **Bedrock**：[`bedrock.go`](internal/gateway/bedrock.go) 现为占位，需接入 `aws-sdk-go-v2` `bedrockruntime` 的 `Converse`，对齐 [`BedrockGatewayService`](../aigc-server/src/main/java/com/example/aigc/service/BedrockGatewayService.java)  
- [ ] **Vertex**：核对 [`VertexAiGatewayService`](../aigc-server/src/main/java/com/example/aigc/service/VertexAiGatewayService.java) 的请求/响应与 OAuth 刷新  
- [ ] **OneLink**：远程模型列表与超时、回退（`application.yml` 中 `onelinkai.*`）  
- [ ] **Azure OpenAI**：若生产使用，补 [`ProviderHttpGateway`](../aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java) 中 Azure 分支  

---

## 横切与工程化

- [ ] **契约清单**：按 `api/index.ts` 列出「方法 + 路径 + DTO」对照表（可用 `TRACKING.yaml` 或代码注释索引，避免散落文档）  
- [ ] **集成测试**：HTTP 层 + 可选 testcontainers MySQL；复用 Java 集成测试场景抽象出的 fixture  
- [ ] **对比验收**：扩展 `parity` 脚本覆盖 `/generate`、连接 CRUD 等（固定种子数据 + 规范化时间戳字段）  
- [ ] **Actuator**：若需与 Java 对齐，补 `GET /actuator/health`（或文档说明由 K8s 探针替代）  
- [ ] **`go build` CI**：在可访问 `proxy.golang.org` 的环境验证依赖与交叉编译  

---

## 建议优先级（简版）

1. 跑通依赖与 CI → 文件下载 → 连接/模型完整 CRUD 与 probe  
2. 剧本工程 CRUD + refine 一条链 → 再铺 `ScriptWorkflow` 其余接口  
3. Router 代理（含 SSE）→ 视频管线与资产历史  
4. Bedrock 真接入 → 全量 parity 脚本  

完成某项后，可直接在本文件对应条目前打勾或删除该行，避免与计划文件重复维护。
