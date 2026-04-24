# 支持 video-kling-v3 视频模型

## 问题描述

当用户尝试使用 `video-kling-v3` 模型通过 OneLink 连接生成视频时，系统报错：

```
OneLink 连接暂不支持该视频模型: video-kling-v3
```

## 根因分析

在 `GenerationServiceImpl.java` 中，`isKlingModel()` 方法只检查 `kling-` 前缀：

```java
private boolean isKlingModel(String modelName) {
    if (modelName == null || modelName.isBlank()) {
        return false;
    }
    return modelName.trim().toLowerCase(Locale.ROOT).startsWith("kling-");
}
```

当 `modelName = "video-kling-v3"` 时，`startsWith("kling-")` 返回 `false`，导致代码无法进入 Kling 视频生成分支，从而抛出异常。

## 修复方案

修改 `isKlingModel()` 方法，使其同时支持：
- `kling-` 前缀（如 `kling-v1`, `kling-v1-6`）
- `video-kling-` 前缀（如 `video-kling-v3`, `video-kling-v3-6`）

## 实施步骤

1. 修改 `GenerationServiceImpl.java` 中的 `isKlingModel()` 方法
2. 将判断逻辑改为同时检查两种前缀

## 修改文件

- `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`

## 修改内容

将第 904-909 行的方法从：

```java
private boolean isKlingModel(String modelName) {
    if (modelName == null || modelName.isBlank()) {
        return false;
    }
    return modelName.trim().toLowerCase(Locale.ROOT).startsWith("kling-");
}
```

修改为：

```java
private boolean isKlingModel(String modelName) {
    if (modelName == null || modelName.isBlank()) {
        return false;
    }
    String normalized = modelName.trim().toLowerCase(Locale.ROOT);
    return normalized.startsWith("kling-") || normalized.startsWith("video-kling-");
}
```
