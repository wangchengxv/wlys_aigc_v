# Tasks

- [x] Task 1: 在 catalog.go 中注册 vidu_onelink Provider
  - [x] 定义 `vidu_onelink` Provider，Key 为 `vidu_onelink`，DisplayName 为 `OneLinkAI Vidu`
  - [x] DefaultBaseURL 为 `https://api.onelinkai.cloud`
  - [x] AuthMode 为 `AuthBearer`，VideoSubmitPath 为 `/vidu/ent/v2/img2video`，VideoResultPath 为 `/vidu/ent/v2/tasks/{taskId}/creations`
  - [x] 注册别名：`onelinkvidu`、`OneLinkVidu`、`viduai-onelink`

- [x] Task 2: 扩展 genVideos 路由支持 onelinkai Vidu 模型
  - [x] 在 `genVideos` 中识别 `vidu_onelink` Provider 并走 OneLinkAI Vidu 路径
  - [x] 复用现有 `callViduVideo` 方法发起 OneLinkAI Vidu 调用
  - [x] 确保 `vidu_onelink` 向后兼容路径逻辑存在

- [x] Task 3: 扩展预置模型列表
  - [x] 在 `preset_models.go` 中补充 OneLinkAI Vidu 预置模型条目
  - [x] 覆盖 `viduq3-turbo`、`viduq3-pro`、`viduq2-pro-fast`、`viduq2-pro`、`viduq2-turbo`、`viduq2`、`viduq1`、`viduq1-classic`、`vidu2.0` 等模型
  - [x] Provider 设为 `vidu_onelink`，BaseURL 设为 `https://api.onelinkai.cloud`

- [x] Task 4: 验证与回归测试
  - [x] 确认 OneLinkAI Vidu 模型可被 `resolveModel("video", ...)` 识别
  - [x] 确认 OneLinkAI Vidu 路由到正确路径 `/vidu/ent/v2/img2video`
  - [x] 确认现有直接 Vidu、Ark、Moark 路径无回归
  - [x] 对照 `checklist.md` 完成验收勾选

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 2 and Task 3