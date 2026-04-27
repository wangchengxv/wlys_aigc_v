# aigc-springai-server

基于 `Spring Boot + Spring AI` 的后端工程骨架，目标是把文本调用统一到 `TextGenerationFacade`，并保留回退网关机制，提升可维护性。

## 已实现能力
- 统一文本门面：`TextGenerationFacade`
- 动态客户端工厂：`SpringAiClientFactory`（含简单缓存）
- 回退机制：`FallbackGateway`（默认 `MockFallbackGateway`）
- 兼容接口：
  - `POST /v1/chat/completions`
  - `POST /v1/messages`
  - `GET /v1/models`

## 启动
```bash
cd aigc-springai-server
export OPENAI_API_KEY=你的key
mvn spring-boot:run
```

默认端口：`8090`

## 说明
- 当前是“可运行的迁移骨架”，用于承接你文档里的架构目标。
- 业务路由、供应商细节、结构化解析等可在此基础上继续按规范扩展。
