# Tasks

## Task 1: 扩展 ProviderCatalog 新增 Kling 服务商定义
**目标**: 在 ProviderCatalog.java 中注册 Kling 服务商，使其可以被前端展示和用户选择

- [x] 1.1 查看现有 `vidu_onelink` 的 ProviderDefinition 定义作为参考
- [x] 1.2 创建 `kling` ProviderDefinition，defaultBaseUrl 为 `https://api.onelinkai.cloud`
- [x] 1.3 设置 videoSubmitPath 为 `/v1/videos/text2video`（文生视频）
- [x] 1.4 设置 videoImageSubmitPath 为 `/v1/videos/image2video`（图生视频）
- [x] 1.5 设置 videoResultPath 为 `/v1/videos/tasks/{taskId}`（任务查询）
- [x] 1.6 设置 authMode 为 `BEARER`（Authorization: Bearer {apiKey}）
- [x] 1.7 在 PROVIDER_DEFINITIONS 列表中注册新的 Kling 定义

**验证**: 调用获取服务商目录 API，确认返回包含 kling 类型

## Task 2: 在 GenerationServiceImpl 中新增 Kling 视频生成路由
**目标**: 扩展视频生成逻辑，支持根据模型配置和输入类型路由到正确的 Kling 接口

- [x] 2.1 查看 GenerationServiceImpl 中现有的 `generateVideosWithViduOneLinkConnection` 方法
- [x] 2.2 创建 `generateVideosWithKlingConnection` 方法处理 Kling 视频生成
- [x] 2.3 在 `generateVideos` 方法中添加 Kling 的路由判断分支
- [x] 2.4 实现根据模型配置和是否有参考图选择文生视频或图生视频接口
- [x] 2.5 实现模型模式与输入冲突的业务错误返回
- [x] 2.6 实现 Kling 任务提交逻辑

**验证**: 使用 Kling 模型发起视频生成请求，确认路由到正确的接口

## Task 3: 实现 Kling 异步任务轮询与结果解析
**目标**: 支持 Kling 视频任务的异步提交与结果获取

- [x] 3.1 创建 `pollKlingVideoTask` 方法处理 Kling 任务轮询
- [x] 3.2 实现从 Kling 响应中提取任务 ID、状态、失败原因与视频 URL
- [x] 3.3 将 Kling 结果映射到现有统一视频结果结构
- [x] 3.4 确保不会破坏 Ark、Vidu、Moark 的既有解析逻辑

**验证**: 提交一个 Kling 视频任务，轮询获取结果，确认视频 URL 可用

## Task 4: 在预置模型中添加 Kling 模型示例
**目标**: 为用户提供快捷的 Kling 模型配置入口

- [x] 4.1 查看 PresetModelRegistry 中现有的预置模型定义
- [x] 4.2 添加至少一组 Kling 预置模型（如 kling-1-standard, kling-1-pro）
- [x] 4.3 为预置模型设置正确的视频输入模式（text_to_video/image_to_video）

**验证**: 调用获取预置模型 API，确认返回包含 Kling 模型

## Task 5: 回归验证
**目标**: 确保新增功能不会破坏现有功能

- [x] 5.1 Go 代码编译通过
- [ ] 5.2 验证服务商目录 API 返回正确的 Kling 类型
- [ ] 5.3 验证 Vidu (OneLink)、Ark、Moark 的视频生成功能无回归
- [ ] 5.4 验证连接配置、模型配置的创建、更新、读取流程正常
- [ ] 5.5 验证错误场景（缺少参考图、模型配置冲突）返回正确的错误消息

---

# Task Dependencies

```
Task 1 ──┬──> Task 2 ──> Task 3 ──> Task 5
         │
         └──────> Task 4 ──> Task 5
```

- Task 2 依赖 Task 1 完成服务商定义
- Task 3 依赖 Task 2 完成路由逻辑
- Task 4 可与 Task 2 并行进行
- Task 5 依赖所有其他任务完成后进行验证