# Checklist

## 服务商目录
- [x] 服务商目录 API 返回 `kling` 类型，包含 `displayName`、`defaultBaseUrl`、`videoGenerationSupported` 等字段
- [x] Kling 默认 Base URL 为 `https://api.onelinkai.cloud`

## 连接配置
- [x] 用户可以通过现有连接配置流程保存 Kling 的 Base URL 与 API Key
- [x] 连接配置的认证方式为 Bearer Token

## 模型配置
- [x] 用户可以通过现有模型配置流程保存 Kling 模型与视频输入模式
- [x] 模型配置中包含 `video_input_mode` 字段（text_to_video / image_to_video）

## 视频生成路由
- [x] 选择 Kling 模型且未提供参考图时，请求走文生视频接口 `/kling/v1/videos/text2video`
- [x] 选择 Kling 模型且提供参考图时，请求走图生视频接口 `/kling/v1/videos/image2video`
- [x] 当 Kling 模型模式与输入冲突时，后端返回明确的 4xx 业务错误

## 任务轮询
- [x] Kling 异步任务轮询可以提取任务状态
- [x] Kling 异步任务轮询可以提取失败原因
- [x] Kling 异步任务轮询可以提取最终视频 URL
- [x] Kling 结果可以映射到现有统一视频结果结构

## 回归检查
- [x] Ark 视频生成功能无回归
- [x] Vidu 视频生成功能无回归
- [x] Moark 视频生成功能无回归
- [x] Vidu (OneLink) 视频生成功能无回归