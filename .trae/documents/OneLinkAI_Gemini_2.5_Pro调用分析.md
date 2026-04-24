# OneLinkAI Gemini 2.5 Pro 调用方式总结

## 一、模型注册配置

### 1. Java 后端 - `aigc-server`
**文件路径**: [PresetModelRegistry.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java#L17)

```java
new PresetModel("onelinkai", "gemini-2.5-pro", "https://api.onelinkai.cloud", "OneLinkAI Gemini 2.5 Pro", List.of("text")),
```

**配置说明**:
- Provider: `onelinkai`
- 模型名称: `gemini-2.5-pro`
- API 基础地址: `https://api.onelinkai.cloud`
- 显示名称: `OneLinkAI Gemini 2.5 Pro`
- 能力标签: `["text"]` - 仅文本生成

### 2. Java 后端 - `aigc-server`
**文件路径**: [ProviderCatalog.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java#L38-L75)

```java
register(new ProviderDefinition(
    "onelinkai",
    "OneLinkAI",
    "https://api.onelinkai.cloud",
    "openai",
    "/v1/chat/completions",     // Chat API 路径
    "/v1/models",               // 模型列表路径
    AuthMode.BEARER,             // Bearer Token 认证
    true,                        // textProxySupported = true
    "/v1/images/generations",    // 图片生成路径
    null,                        // 视频提交路径
    null,                        // 视频查询路径
    GatewayKind.OPENAI_COMPAT,   // OpenAI 兼容网关
    List.of(
        "gpt-4o",
        "gpt-4o-mini",
        "claude-sonnet-4-6",
        "gemini-2.5-pro",        // 模型名在静态模型列表中
        "gemini-2.5-flash",
        // ... 其他模型
    ),
    null,
    false
), "onelink", "onelink-ai", "一键AI");
```

**关键配置**:
- `GatewayKind.OPENAI_COMPAT`: 使用 OpenAI 兼容协议
- `AuthMode.BEARER`: 使用 Bearer Token 认证
- `textProxySupported = true`: 支持文本代理
- `chatPath = "/v1/chat/completions"`: Chat Completions API 端点

### 3. Go 后端 - `aigc-server-go`
**文件路径**: [catalog.go](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server-go/internal/catalog/catalog.go#L127-L134)

```go
reg(Provider{
    Key: "onelinkai", DisplayName: "OneLinkAI", DefaultBaseURL: "https://api.onelinkai.cloud",
    APIFormat: "openai", ChatPath: "/v1/chat/completions", ModelsPath: "/v1/models",
    AuthMode: AuthBearer, TextProxySupported: true, Kind: KindOpenAICompat,
    StaticModels: []string{"gpt-4o", "gpt-4o-mini", "claude-sonnet-4-6", "gemini-2.5-pro", "gemini-2.5-flash",
        "wanx-v1", "MiniMax-M2.1", "viduq3-turbo", "video-viduq3-pro", "image-vidu-q2-fast", "image-vidu-q2", "viduq2-turbo",
        "viduq2", "viduq1", "viduq1-classic", "vidu2.0"},
}, "onelink", "onelink-ai", "一键ai")
```

**文件路径**: [preset_models.go](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server-go/internal/service/preset_models.go#L22)

```go
{"onelinkai", "gemini-2.5-pro", "https://api.onelinkai.cloud", "OneLinkAI Gemini 2.5 Pro", []string{"text"}},
```

### 4. React 前端 - `aigc-site-react`
**文件路径**: [index.ts](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-site-react/src/api/index.ts#L969)

```typescript
{ provider: 'onelinkai', modelName: 'gemini-2.5-pro', baseUrl: 'https://api.onelinkai.cloud', displayName: 'OneLinkAI Gemini 2.5 Pro', capabilities: ['text'] },
```

---

## 二、调用链路分析

### 1. 入口点 - 文本生成请求
**文件路径**: [GenerationServiceImpl.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java#L253-L280)

```
generateTextContent()
  └─> resolveModel("text", null)           // 解析模型配置
  └─> buildTextRequest()                   // 构建请求 payload
  └─> providerHttpGateway.invokeChat()     // 调用网关
```

### 2. HTTP 网关调用
**文件路径**: [ProviderHttpGateway.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java#L52-L81)

```java
public Map<String, Object> invokeChat(...) {
    // 1. 对于 OPENAI_COMPAT 类型网关，走标准 OpenAI 协议
    return postJson(baseUrl, definition.chatPath(), definition, apiKey, meta, payload, timeout);
}
```

### 3. HTTP 请求构建
**文件路径**: [ProviderHttpGateway.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java#L540-L554)

```java
private HttpRequest.Builder buildJsonRequest(...) {
    HttpRequest.Builder builder = HttpRequest.newBuilder(buildUri(baseUrl, path, connectionMetadata))
        .timeout(timeout)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
    applyHeaders(builder, definition, apiKey, connectionMetadata);
    return builder;
}
```

### 4. 认证头设置
**文件路径**: [ProviderHttpGateway.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java#L557-L572)

```java
private void applyHeaders(...) {
    // BEARER 认证模式
    if (definition.authMode() == AuthMode.BEARER && apiKey != null && !apiKey.isBlank()) {
        builder.header("Authorization", "Bearer " + apiKey);
    }
}
```

---

## 三、完整请求流程

```
客户端请求
    │
    ▼
[GenerationServiceImpl.generateTextContent()]
    │
    ▼
[ModelCapabilityService.resolveModel("text")] 
    │   从 ConnectionConfig 和 ModelConfig 解析
    │   - provider: "onelinkai"
    │   - modelName: "gemini-2.5-pro"
    │   - baseUrl: 用户配置的 OneLinkAI 连接地址
    │   - apiKey: 解密后的 API Key
    │
    ▼
[buildTextRequest(prompt, style, textLength, resolvedModel)]
    │   构建 OpenAI 格式的请求体
    │   {
    │     "model": "gemini-2.5-pro",
    │     "messages": [...],
    │     ...
    │   }
    │
    ▼
[ProviderHttpGateway.invokeChat()]
    │
    ▼
[buildJsonRequest()]
    │   - baseUrl: https://api.onelinkai.cloud
    │   - path: /v1/chat/completions
    │   - headers: {
    │       "Content-Type": "application/json",
    │       "Authorization": "Bearer <apiKey>"
    │     }
    │
    ▼
HTTP POST 请求
    https://api.onelinkai.cloud/v1/chat/completions
```

---

## 四、涉及的代码文件汇总

| 层级 | 文件路径 | 作用 |
|------|----------|------|
| **前端** | `aigc-site-react/src/api/index.ts` | 模型配置定义和 API 接口 |
| **前端** | `aigc-site-react/src/pages/SettingsPage.tsx` | OneLinkAI 连接配置页面 |
| **前端** | `aigc-site-react/src/components/model/ModelForm.tsx` | 模型选择表单组件 |
| **Java后端** | `aigc-server/.../model/PresetModelRegistry.java` | 预置模型注册表 |
| **Java后端** | `aigc-server/.../service/ProviderCatalog.java` | 提供商目录定义 |
| **Java后端** | `aigc-server/.../service/impl/GenerationServiceImpl.java` | 文本生成核心逻辑 |
| **Java后端** | `aigc-server/.../service/ProviderHttpGateway.java` | HTTP 网关，执行实际 API 调用 |
| **Java后端** | `aigc-server/.../service/ModelCapabilityService.java` | 模型能力解析 |
| **Go后端** | `aigc-server-go/internal/catalog/catalog.go` | Go 版本提供商目录 |
| **Go后端** | `aigc-server-go/internal/service/preset_models.go` | Go 版本预置模型 |

---

## 五、关键配置点

1. **连接配置**: 用户需要在设置中配置 OneLinkAI 连接，包含：
   - Base URL: `https://api.onelinkai.cloud`
   - API Key: OneLinkAI 平台的 API Key

2. **模型选择**: 选择 `onelinkai` provider + `gemini-2.5-pro` 模型

3. **认证方式**: Bearer Token (`Authorization: Bearer <apiKey>`)

4. **API 端点**: `POST https://api.onelinkai.cloud/v1/chat/completions`

5. **请求格式**: OpenAI Chat Completions 兼容格式
