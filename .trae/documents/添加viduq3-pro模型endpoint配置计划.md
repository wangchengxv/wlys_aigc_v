# Vidu Q3 Pro 模型配置完善计划

## 需求分析
用户需要在后端添加 viduq3-pro 模型的 HTTP POST 请求地址：`https://api.onelinkai.cloud/vidu//vidu/vidu/ent/v2/img2video`

## 现状分析
当前 `PresetModel` 类结构：
- `provider`: 提供商标识（如 "vidu_onelink"）
- `modelName`: 模型名称
- `baseUrl`: API 基础地址
- `displayName`: 显示名称
- `capabilities`: 能力列表（image/video/text）

问题：`PresetModel` 没有专门的 `endpoint` 字段来存储自定义 API 端点。

## 实现方案

### 方案：扩展 PresetModel 类添加 endpoint 字段（推荐）

#### 步骤 1: 修改 PresetModel.java
在 `PresetModel` 类中添加：
- 新字段：`private final String endpoint;`
- 修改构造函数支持新字段
- 添加 getter 方法：`getEndpoint()`
- 添加判断方法：`hasEndpoint()` 检查是否有自定义端点

#### 步骤 2: 修改 PresetModelRegistry.java
在现有的 `vidu_onelink` 提供商中添加 viduq3-pro 模型配置：
```java
new PresetModel("vidu_onelink", "viduq3-pro-img2video", "https://api.onelinkai.cloud", 
                "OneLinkAI Vidu Q3 Pro Img2Video", List.of("image", "video"), 
                "/vidu//vidu/vidu/ent/v2/img2video"),
```

#### 步骤 3: 更新 API 调用逻辑（可选）
在实际的 API 调用代码中，需要检查模型是否有自定义 endpoint：
- 如果有自定义 endpoint，使用 `baseUrl + endpoint` 进行请求
- 如果没有，使用原有的 `baseUrl` 进行请求

## 实施细节

### 步骤 1：修改 PresetModel.java
- 文件位置：`/Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/model/PresetModel.java`

### 步骤 2：修改 PresetModelRegistry.java
- 文件位置：`/Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
- 在第63行之后添加新的模型配置

### 步骤 3：验证更改
- 检查 PresetModel 类的所有使用位置
- 确保向后兼容性（可选 endpoint 为 null）
