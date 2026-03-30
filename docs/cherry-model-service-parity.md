# Cherry「模型服务」与 AIGCmanju 对齐说明

本文档对应里程碑 **M0**：Cherry Studio「设置 → 模型服务」能力清单、Cherry → AIGC 字段映射，以及验收用例（Given / When / Then）。实现以 `ProviderCatalog` 已注册的服务商为准。

## 1. Cherry 能力清单（对照项）

| 能力域 | Cherry 参考 | AIGC 落点 |
|--------|-------------|-----------|
| 服务商列表 | 左栏列表、搜索、添加 | `ProviderHubPage` 侧栏 + 向导 |
| 连接身份 | 名称、Base URL、API Key（或类型专属凭证） | `ConnectionForm` + `ConnectionConfig` |
| 服务商专属字段 | Azure apiVersion、Bedrock region/AK/SK、Vertex 项目/区域/SA JSON 等 | `metadata` + `ProviderConnectionFields` / `panels/*` |
| 高级请求选项 | 自定义 Header、额外 Query | `metadata.customHeadersJson`、`metadata.customQueryParamsJson` → `ProviderHttpGateway` 合并 |
| 多密钥轮换 | 多 Key 列表 | `metadata.extraApiKeys`（加密换行分隔）+ `RouterProxyService` 同连接内 401 重试 |
| 模型列表 | 搜索、分组、健康检查、批量导入 | `ProviderHubPage` + `probeModel` API |
| OAuth / Copilot / OVMS | 桌面 OAuth、模型下载 | 见 §4，Web 侧以说明与 OpenAI 兼容代理替代 |

## 2. Cherry → AIGC 字段映射

### 2.1 连接 `ConnectionConfig`

| Cherry 概念 | AIGC 字段 | 说明 |
|-------------|-----------|------|
| Provider id | `provider` | 与目录 `key` 一致，如 `openai`、`azure_openai` |
| Base URL | `baseUrl` | 含协议与路径前缀 |
| API Key | `encryptedApiKey`（存储）/ 表单 `apiKey` | Bedrock 时密钥字段存 Secret Access Key |
| 启用 | `enabled` | |
| Azure API 版本 | `metadata.apiVersion` | 默认 `2024-06-01` |
| Bedrock Region / AK / Session | `metadata.region`、`metadata.awsAccessKeyId`、`metadata.awsSessionToken` | Session 加密存储 |
| Vertex 项目 / 区域 / SA | `metadata.vertexProjectId`、`metadata.vertexLocation`、`metadata.vertexServiceAccountJson` | SA JSON 加密 |
| 自定义 Header | `metadata.customHeadersJson` | JSON 字符串，`[{"name":"X-Custom","value":"..."}]`，服务端加密 |
| 额外 Query | `metadata.customQueryParamsJson` | JSON 对象字符串，如 `{"debug":"1"}`，服务端加密 |
| 附加 API Key（轮换） | `metadata.extraApiKeys` | 换行分隔的多个密钥，整段加密 |

### 2.2 模型 `ModelConfig.metadata`

| Cherry 概念 | AIGC 字段 | 说明 |
|-------------|-----------|------|
| 分组 group | `metadata.group` | 字符串，用于列表分组展示 |
| 备注 notes | `metadata.notes` | 可选说明 |
| 模型类型 | `metadata.modelType` | 如 `chat`、`embedding`（展示用） |
| 能力 | `metadata.capabilities` | `text` / `image` / `video` / `embedding` / `rerank` |

## 3. Given / When / Then 验收用例（节选）

### 3.1 连接与专属字段

- **G** 用户已打开「服务商中心」并新建连接，目录为 `azure_openai`。  
  **W** 填写 Base URL、apiVersion、密钥并保存。  
  **T** 连接测试中列表模型或聊天代理使用 `api-version` 查询参数与部署名一致。

- **G** 连接为 `aws_bedrock`，metadata 含 region、Access Key、Secret（密钥字段）、可选 Session。  
  **W** 执行连接测试或路由文本请求。  
  **T** 请求经 Bedrock 网关，无因缺字段导致的 400。

- **G** 连接为 `vertex_ai` 且 SA JSON 有效。  
  **W** 发起需要 Vertex 的调用。  
  **T** 使用 GCP 凭据成功（或返回供应商侧明确错误而非本地解析失败）。

### 3.2 自定义 Header / Query

- **G** 在连接 metadata 中配置 `customHeadersJson` 含 `Authorization: Bearer xxx` 或业务 Header。  
  **W** 通过 `ProviderHttpGateway` 发起 OpenAI 兼容请求。  
  **T** 出站请求包含合并后的 Header（在默认鉴权之后应用，允许覆盖）。

- **G** 配置 `customQueryParamsJson`。  
  **W** 调用 `listModels` 或 `chat` 的 HTTP 路径。  
  **T** 最终 URL 附带额外 query 参数。

### 3.3 模型列表与运维

- **G** 多个模型设置了不同的 `metadata.group`。  
  **W** 打开服务商中心模型区。  
  **T** 表格按分组折叠/分块展示，未设置组的归入「未分组」。

- **G** 连接下有多条已启用模型。  
  **W** 点击「批量健康检查」。  
  **T** 弹窗展示每条模型的探测结果（成功/失败与消息）。

- **G** 连接测试返回远端模型 ID 列表。  
  **W** 勾选部分 ID 后批量导入。  
  **T** 仅所选模型创建为本地配置，重复 ID 跳过或提示。

### 3.4 多密钥轮换

- **G** 主密钥有效或第一个附加密钥有效，其余无效。  
  **W** 通过路由代理发起聊天。  
  **T** 在 401 时自动尝试下一密钥直至成功或耗尽。

### 3.5 桌面能力缺口

- **G** 用户需要 Copilot / CherryIN OAuth 或 OVMS 下载模型。  
  **W** 阅读服务商中心说明或调用 `/api/v1/provider-catalog/oauth-notes`。  
  **T** 明确说明 Web 栈不支持桌面 OAuth/OVMS，并给出 OpenAI 兼容代理或 Cherry 客户端建议。

---

*文档随实现迭代更新；不修改 `.cursor/plans` 内计划文件本体。*
