# Checklist

- [x] `catalog.go` 中 `vidu_onelink` Provider 注册完整（Key、DisplayName、DefaultBaseURL、AuthMode、VideoSubmitPath、VideoResultPath、别名）
- [x] `genVideos` 中能识别 `vidu_onelink` Provider 并路由到 OneLinkAI Vidu 路径
- [x] `callViduVideo` 可接受 `vidu_onelink` Provider 并正确发起请求
- [x] `vidu_onelink` 向后兼容路径逻辑存在且可工作
- [x] `preset_models.go` 中 OneLinkAI Vidu 预置模型条目完整（Provider、ModelName、BaseURL、DisplayName、Caps）
- [x] 现有直接 Vidu 连接路由无回归
- [x] 现有 Ark、Moark 路由无回归
- [x] 代码编译通过
- [x] Vidu 相关测试全部通过（5/5 tests passed）