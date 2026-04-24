# Java后端新增 qwen-turbo-latest 模型支持计划

## 需求分析

在 Java 后端增加与 Go 后端相同的 qwen-turbo-latest 模型配置。需要修改两处文件。

## 实施步骤

### 步骤 1：更新 Java ProviderCatalog.java

**文件路径**：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`

**修改内容**：在 `qwen` Provider 的 `staticModels` 中添加 `qwen-turbo-latest`

```java
// 修改位置：第130行附近
// 修改前
List.of("qwen-max", "qwen-plus", "qwen-turbo", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-7b-instruct"),

// 修改后
List.of("qwen-max", "qwen-plus", "qwen-turbo", "qwen-turbo-latest", "qwen-long", "qwen2.5-72b-instruct", "qwen2.5-7b-instruct"),
```

---

### 步骤 2：更新 Java PresetModelRegistry.java

**文件路径**：`aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`

**修改内容**：在 qwen 相关的 preset models 中添加新的条目

```java
// 修改位置：在 qwen-turbo 条目后添加
new PresetModel("qwen", "qwen-turbo", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Turbo", List.of("text")),
new PresetModel("qwen", "qwen-turbo-latest", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Turbo 最新版", List.of("text")),
new PresetModel("qwen", "qwen-max", "https://dashscope.aliyuncs.com/compatible-mode", "通义千问 Max", List.of("text")),
```

---

## 配置一致性检查

| 配置项 | Go 后端 | Java 后端 |
|--------|---------|-----------|
| Provider Key | qwen | qwen |
| Base URL | https://dashscope.aliyuncs.com/compatible-mode | https://dashscope.aliyuncs.com/compatible-mode |
| Model Name | qwen-turbo-latest | qwen-turbo-latest |
| Auth Mode | AuthBearer | AuthMode.BEARER |
| API Format | openai | openai |
| Chat Path | /v1/chat/completions | /v1/chat/completions |
| Gateway Kind | OPENAI_COMPAT | OPENAI_COMPAT |
| Display Name | 通义千问 Turbo 最新版 | 通义千问 Turbo 最新版 |
| Capabilities | text | text |

---

## 影响范围

- **路由和代理**：模型可被正确路由到通义千问端点
- **快捷配置**：前端模型选择器中可选择新模型
- **预设模型列表**：API 返回的预设模型列表包含新模型

---

## 测试验证

1. 重新编译 Java 服务
2. 验证 `/api/v1/provider-catalog` 接口返回 qwen-turbo-latest
3. 验证 `/api/v1/preset-models` 接口返回新的预设模型条目
4. 测试通过快捷配置向导使用新模型
