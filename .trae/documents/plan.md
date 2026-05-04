# 剧本闭环 Lite 两项问题修复计划

## 问题一：三视图提示词功能无法由剧本直接调用大模型自主抽取识别关键帧

### 现状分析

当前流程：
1. 用户在 `Step2Script.tsx` 填写"三视图提示词"字段（可选）
2. 用户不填时，系统自动拼接提示词模板 + 剧本内容（前1200字）
3. 直接调用 `ImageGenerationCapabilityService.generateImages()` 生成图片
4. **缺少 LLM 文本分析环节**：无法从剧本中提取角色特征、动作描述、场景要素等关键信息来优化提示词

用户期望：当"三视图提示词"字段为空时，系统能够通过大模型（GPT-4o/Claude 等）分析剧本内容，自主生成更精准的三视图提示词，再调用文生图。

### 修复方案

#### 步骤 1：在后端 `StoryboardLiteService` 中新增 LLM 提示词生成逻辑

**文件**: `aigc-server/src/main/java/com/example/aigc/service/StoryboardLiteService.java`

1. 注入 `ProviderHttpGateway`（已有 LLM 调用能力）
2. 在 `generateKeyframes()` 方法中，当 `request.prompt()` 为空时：
   - 先调用 `ProviderHttpGateway.invokeChat()` 使用 GPT-4o/Claude 分析剧本
   - 提示词模板可参考 `three-view-character-user.md`
   - 将 LLM 返回的优化后提示词用于图片生成

#### 步骤 2：新增提示词模板文件

**文件**: `aigc-server/src/main/resources/prompts/visual/three-view-extraction-user.md`

用于指导 LLM 从剧本中提取角色三视图描述的提示词模板。

#### 步骤 3（可选）：暴露配置项让用户选择是否启用 LLM 增强

在 `GenerateKeyframesRequest` DTO 中增加 `useLlmEnhancement` 字段，或通过全局开关控制。

### 关键代码修改位置

- [StoryboardLiteService.java#L85-139](file:///Users/xingyi/Downloads/aigc/AIGC_university/aigc-server/src/main/java/com/example/aigc/service/StoryboardLiteService.java#L85-L139)：`generateKeyframes()` 方法
- 注入 `ProviderHttpGateway` 依赖
- 新增 `extractThreeViewPromptWithLlm()` 私有方法

---

## 问题二：从已确定关键帧生成视频报错 —— reference_image URL 校验失败

### 现状分析

错误信息：`{code: 400, message: "OneLink 豆包视频 reference_image 必须为可访问的 http(s) 地址", data: null}`

#### 问题根源

在 `StoryboardLiteService.resolveVideoReferenceImage()` 方法（第375-397行）中：

```java
private String resolveVideoReferenceImage(StoryboardLiteKeyframe keyframe) {
    String direct = trimToNull(keyframe.imageUrl);
    if (direct != null && (direct.startsWith("http://") || direct.startsWith("https://") || direct.startsWith("data:image/"))) {
        return direct;  // 直接返回 /api/v1/files/xxx 这样的路径！
    }
    // ...
}
```

当关键帧图片已落盘到本地存储时，`imageUrl` 格式为 `/api/v1/files/{fileId}`，**不满足** `http://` 或 `https://` 前缀校验。

接着在 `GenerationServiceImpl.callOneLinkDoubaoVideoApi()` 中：
- `isValidMediaUrl()` 要求 URL 必须以 `http://` 或 `https://` 开头
- `/api/v1/files/...` 路径不满足条件，抛出 400 错误

#### 修复方案

**方案 A（推荐）**：在 `StoryboardLiteService.resolveVideoReferenceImage()` 中将本地文件路径转换为可访问的 HTTP URL

1. 在 `StoryboardLiteService` 中注入 `MediaStorageService`（已有 `toPublicUrl` 方法）
2. 当 `direct` 是 `/api/v1/files/...` 格式时，替换为完整服务器 URL

需要解决：如何获取服务器 base URL（跨环境适配）

**方案 B**：在 `resolveVideoReferenceImage()` 中强制使用 data URI（已有 base64 逻辑但未触发）

当前代码在 `direct` 不满足 `startsWith("http")` 时，会尝试读取文件并转为 base64。问题是 `/api/v1/files/...` 走不到 base64 分支，因为第380行的判断：
```java
if (direct != null && (direct.startsWith("http://") || direct.startsWith("https://") || direct.startsWith("data:image/"))) {
    return direct;  // 直接返回，不走 base64 逻辑
}
```

**方案 C（最简修复）**：在 `resolveVideoReferenceImage()` 中，将 `/api/v1/files/...` 路径也当作 fileId 处理，读取文件并转为 base64 data URI

这样可确保发送给豆包视频 API 的始终是 `data:image/...;base64,...` 格式，绕过 URL 可访问性校验。

### 关键代码修改位置

**文件**: `aigc-server/src/main/java/com/example/aigc/service/StoryboardLiteService.java`

修改 `resolveVideoReferenceImage()` 方法（第375-397行）：

```java
private String resolveVideoReferenceImage(StoryboardLiteKeyframe keyframe) {
    if (keyframe == null) {
        return null;
    }
    String direct = trimToNull(keyframe.imageUrl);

    // 处理 /api/v1/files/... 路径
    if (direct != null && direct.startsWith("/api/v1/files/")) {
        String fileId = trimToNull(direct.substring("/api/v1/files/".length()));
        StoredFileRecord record = entityManager.find(StoredFileRecord.class, fileId);
        if (record != null && localAssetFileService.exists(record)) {
            String mediaType = firstNonBlank(trimToNull(record.mediaType), "image/png");
            byte[] bytes = localAssetFileService.readBytes(record);
            return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        return direct;
    }

    // 处理 http/https/data URI
    if (direct != null && (direct.startsWith("http://") || direct.startsWith("https://") || direct.startsWith("data:image/"))) {
        return direct;
    }

    // 处理 fileId 裸路径
    String fileId = trimToNull(direct);
    if (fileId == null) {
        return direct;
    }
    StoredFileRecord record = entityManager.find(StoredFileRecord.class, fileId);
    if (record == null || !localAssetFileService.exists(record)) {
        return direct;
    }
    String mediaType = firstNonBlank(trimToNull(record.mediaType), "image/png");
    byte[] bytes = localAssetFileService.readBytes(record);
    return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes);
}
```

---

## 实施步骤

### 阶段一：修复视频生成 reference_image 错误（优先级高）

1. 修改 `StoryboardLiteService.resolveVideoReferenceImage()` 方法
2. 编写单元测试验证修改（参考 `StoryboardLiteServiceIntegrationTest.java`）
3. 本地测试视频生成流程

### 阶段二：新增 LLM 提示词增强功能

1. 在 `StoryboardLiteService` 中注入 `ProviderHttpGateway`
2. 新增提示词模板文件 `three-view-extraction-user.md`
3. 在 `generateKeyframes()` 中新增 LLM 分析分支
4. 测试 LLM 增强后的三视图生成效果

---

## 涉及的文件

| 文件 | 修改类型 |
|------|----------|
| `aigc-server/src/main/java/com/example/aigc/service/StoryboardLiteService.java` | 修改 |
| `aigc-server/src/main/resources/prompts/visual/three-view-extraction-user.md` | 新增 |
| `aigc-server/src/test/java/com/example/aigc/service/StoryboardLiteServiceIntegrationTest.java` | 修改（测试） |
