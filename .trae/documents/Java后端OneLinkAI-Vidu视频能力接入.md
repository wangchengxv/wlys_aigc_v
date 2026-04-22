# Java 后端 OneLinkAI Vidu 视频能力接入计划

## 目标
在 Java 后端完成 OneLinkAI Vidu 视频能力的正式接入，使 `vidu_onelink` Provider 可被用户直接配置并路由。

## 当前状态分析

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Provider 注册 | ✅ 已完成 | `ProviderCatalog.java:349-365` 已有 `vidu_onelink` 注册 |
| 预置模型 onelinkai 下 Vidu | ✅ 已完成 | `PresetModelRegistry.java:24-32` 已有 onelinkai 下 viduq* 模型 |
| 预置模型 vidu_onelink | ❌ 缺失 | 需要添加 `vidu_onelink` Provider 专属的预置模型条目 |
| GenerationServiceImpl 路由 | ❌ 缺失 | 视频路由只支持 `ark`、`moark`、`vidu`、`onelinkai`，缺少 `vidu_onelink` 直连支持 |

## 实现步骤

### 步骤 1：修改 PresetModelRegistry.java
在 `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java` 中添加 `vidu_onelink` Provider 专属的预置模型条目：
```java
new PresetModel("vidu_onelink", "viduq3-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Turbo", List.of("video")),
new PresetModel("vidu_onelink", "viduq3-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q3 Pro", List.of("video")),
new PresetModel("vidu_onelink", "viduq2-pro-fast", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro Fast", List.of("video")),
new PresetModel("vidu_onelink", "viduq2-pro", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Pro", List.of("video")),
new PresetModel("vidu_onelink", "viduq2-turbo", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2 Turbo", List.of("video")),
new PresetModel("vidu_onelink", "viduq2", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q2", List.of("video")),
new PresetModel("vidu_onelink", "viduq1", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1", List.of("video")),
new PresetModel("vidu_onelink", "viduq1-classic", "https://api.onelinkai.cloud", "OneLinkAI Vidu Q1 Classic", List.of("video")),
new PresetModel("vidu_onelink", "vidu2.0", "https://api.onelinkai.cloud", "OneLinkAI Vidu 2.0", List.of("video"))
```

### 步骤 2：修改 GenerationServiceImpl.java 视频路由
在 `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java` 的视频路由逻辑（大约 336 行附近）中添加 `vidu_onelink` 分支：

在 `if ("vidu".equalsIgnoreCase(...))` 后添加：
```java
if ("vidu_onelink".equalsIgnoreCase(resolvedModel.provider().key())) {
    return new MediaResult(
            resolvedModel.model().getModelName(),
            generateVideosWithViduOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl, videoViduOptions),
            resolvedModel.source(),
            resolvedModel.matchedBy(),
            resolvedModel.rejectReason()
    );
}
```

同时更新错误提示信息，将 `Vidu(vidu)` 改为包含 `vidu_onelink` 的更完整提示。

### 步骤 3：验证与测试
1. 执行 Java 编译（`mvn compile` 或 `gradle compile`）验证代码正确性
2. 如有 Vidu 相关测试，执行测试
3. 确认现有 `ark`、`moark`、`vidu`、`onelinkai` 路径无回归

## 涉及文件
- `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
- `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`

## 依赖
- 无外部依赖
- 依赖于 Go 后端已完成的 `vidu_onelink` Provider 注册