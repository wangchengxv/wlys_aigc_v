# 新增模型支持计划：qwen-turbo-latest

## 需求分析

根据提供的 OpenAPI 规范，需要新增支持 **qwen-turbo-latest** 模型。该模型遵循 OpenAI Chat Completions API 格式，使用 Bearer Token 认证方式。

## 实施步骤

### 步骤 1：更新 Go 后端 catalog.go

**文件路径**：`aigc-server-go/internal/catalog/catalog.go`

**修改内容**：在 `qwen` Provider 的 `StaticModels` 中添加 `qwen-turbo-latest`

```go
// 修改前
StaticModels: []string{"qwen-max", "qwen-plus", "qwen-turbo", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-7b-instruct"}

// 修改后
StaticModels: []string{"qwen-max", "qwen-plus", "qwen-turbo", "qwen-turbo-latest", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-7b-instruct"}
```

**原因**：`StaticModels` 用于定义该 Provider 支持的静态模型列表，添加新模型名称以支持路由和代理。

---

### 步骤 2：更新 Go 后端 preset_models.go

**文件路径**：`aigc-server-go/internal/service/preset_models.go`

**修改内容**：在 `PresetModels` 数组中添加新的预设模型条目

```go
// 在 qwen 相关条目中添加
{"qwen", "qwen-turbo-latest", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Turbo 最新版", []string{"text"}},
```

**原因**：`PresetModels` 提供预置模型的完整配置（包括 BaseURL、显示名称、能力标签），用于快捷配置向导。

---

### 步骤 3：验证配置一致性

**检查项**：
1. 确认 `catalog.go` 中 qwen provider 的 BaseURL 为 `https://dashscope.aliyuncs.com/compatible-mode`
2. 确认认证模式为 `AuthBearer`
3. 确认 API 格式为 `openai`
4. 确认 Chat 路径为 `/v1/chat/completions`

---

## 技术细节

### OpenAPI 规范关键参数

| 参数 | 值 | 说明 |
|------|-----|------|
| BaseURL | `https://dashscope.aliyuncs.com/compatible-mode` | 通义千问兼容模式端点 |
| Auth | Bearer Token | Authorization Header |
| API Format | OpenAI | 兼容 OpenAI 格式 |
| Chat Path | `/v1/chat/completions` | 聊天补全接口 |

### 请求示例

```json
{
  "model": "qwen-turbo-latest",
  "messages": [
    {"role": "user", "content": "你好"}
  ]
}
```

### 响应格式

```json
{
  "id": "xxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "qwen-turbo-latest",
  "choices": [...],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 50,
    "total_tokens": 60
  }
}
```

---

## 影响范围

- **后端影响**：Go 服务路由和代理层
- **前端影响**：模型选择下拉菜单（需确认前端是否从后端动态获取模型列表）
- **用户影响**：可在系统内使用 qwen-turbo-latest 模型进行对话

---

## 测试验证

1. 确认 API 代理可正确路由到通义千问端点
2. 确认模型可被路由策略正确选择
3. 确认快捷配置向导中可选择新模型
