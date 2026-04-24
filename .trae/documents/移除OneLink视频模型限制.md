# 移除 OneLink 视频模型限制计划

## 任务目标
移除 `GenerationServiceImpl.java` 中对 OneLink 视频模型的限制，当前代码仅允许 Vidu 或 Kling 模型通过 OneLink 连接，其他模型会报错。

## 现状分析
**文件位置**: `/Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`

**限制位置**: 第 363-382 行
```java
if ("onelinkai".equalsIgnoreCase(resolvedModel.provider().key())) {
    if (isViduWorkspaceModel(resolvedModel.model().getModelName())) {
        // 调用 Vidu OneLink 生成方法
    }
    if (isKlingModel(resolvedModel.model().getModelName())) {
        // 调用 Kling OneLink 生成方法
    }
    throw new BizException(400, "当前 OneLink 视频模型仅支持 Vidu 或 Kling；其它模型请改用方舟/专属连接或补充接入");
}
```

## 实现方案

### 方案选择
**推荐方案**: 将 OneLink 连接作为通用的视频生成入口，不再限制模型类型

### 实施步骤

1. **修改 OneLink 处理逻辑**
   - 移除第 363-382 行的模型类型检查
   - 将 OneLink provider 作为通用的视频生成连接处理
   - 直接调用相应的生成方法（需要根据模型类型选择）

2. **保留必要的模型判断**
   - 保留 `isViduWorkspaceModel()` 和 `isKlingModel()` 辅助方法（其他位置可能使用）
   - 在 OneLink 分支中，根据模型类型调用不同的生成方法

3. **更新错误提示**
   - 修改第 384 行的错误信息，移除对 OneLink 的限制说明
   - 新的错误信息应反映实际支持的 provider

## 具体代码修改

### 修改前（第 363-382 行）
```java
if ("onelinkai".equalsIgnoreCase(resolvedModel.provider().key())) {
    if (isViduWorkspaceModel(resolvedModel.model().getModelName())) {
        return new MediaResult(
                resolvedModel.model().getModelName(),
                generateVideosWithViduOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl, videoViduOptions),
                resolvedModel.source(),
                resolvedModel.matchedBy(),
                resolvedModel.rejectReason()
        );
    }
    if (isKlingModel(resolvedModel.model().getModelName())) {
        return new MediaResult(
                resolvedModel.model().getModelName(),
                generateVideosWithKlingOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl),
                resolvedModel.source(),
                resolvedModel.matchedBy(),
                resolvedModel.rejectReason()
        );
    }
    throw new BizException(400, "当前 OneLink 视频模型仅支持 Vidu 或 Kling；其它模型请改用方舟/专属连接或补充接入");
}
```

### 修改后
```java
if ("onelinkai".equalsIgnoreCase(resolvedModel.provider().key())) {
    return new MediaResult(
            resolvedModel.model().getModelName(),
            generateVideosWithOneLinkConnection(prompt, count, resolvedModel, videoReferenceImageUrl, videoViduOptions),
            resolvedModel.source(),
            resolvedModel.matchedBy(),
            resolvedModel.rejectReason()
    );
}
```

## 新增方法
需要创建一个通用的 `generateVideosWithOneLinkConnection` 方法，根据模型名称自动选择 Vidu 或 Kling 的生成逻辑。

## 风险评估
- **风险等级**: 低
- **影响范围**: 仅影响使用 `onelinkai` provider 的视频生成请求
- **向后兼容**: 已有的 Vidu/Kling 模型仍能正常工作

## 验证计划
1. 确认代码修改正确无误
2. 检查是否有编译错误
3. 测试 OneLink 连接的视频生成功能（可选）
