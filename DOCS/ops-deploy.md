# Ops & Deploy

## 开发环境基线
- Node.js：20 LTS
- Java：17
- MySQL：8.x
- Redis：7.x
- 对象存储：MinIO（本地）或 OSS（远端）
- 前端端口：3000 或 5173
- 后端端口：8080

## 部署拓扑
```text
CDN -> Nginx -> React 静态资源 + Spring Boot API 集群
                          -> MySQL / Redis / MinIO(OSS) / AI 服务
```

## 运维要点
- API 与任务执行器可拆分部署，避免长任务影响接口响应。
- Redis 用于任务进度缓存与分布式协同。
- 资产统一入对象存储，禁止依赖本地磁盘持久化。
- 关键操作保留审计日志，便于问题追踪与合规审查。
