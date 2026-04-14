# Go / Java 并行与切换说明

## 并行运行（推荐验收方式）

1. **数据库**：仅由一方执行 Flyway 迁移。并行阶段建议继续由现有 **Java `aigc-server`** 启动并完成迁移；Go 服务 `aigc-server-go` 仅连接**同一 MySQL**，不重复执行迁移工具，避免 `flyway_schema_history` 双写。
2. **配置**：对齐环境变量（与 `aigc-server` 的 `application.yml` 注释一致），至少包括：`DB_*`、`AIGC_ACCESS_TOKEN`、`AIGC_USER_ID_REQUIRED`、`ENCRYPTION_KEY`、`ARK_API_KEY`、`AIGC_COMFY_BASE_URL` 等。
3. **端口**：默认 Go 与 Java 均为 `8080`，并行时需将一方改为例如 `SERVER_PORT=8081`（Go）或 Spring `server.port`。
4. **对比测试**：使用 [scripts/parity/compare.sh](scripts/parity/compare.sh) 对同一基线请求 Java 与 Go，核对 HTTP 状态与 JSON `code/message` 形状（动态字段可做规范化后再比）。

## 切换流量

1. 确认 Go 侧已通过集成/对比测试，且加密、文件落盘路径（`AIGC_DATA_DIR`）与 Java 一致。
2. 冻结 schema 变更：最后一次迁移仍由 Java 执行，或改为独立 Flyway 任务；切换后明确**唯一**迁移执行方。
3. 将网关/前端 `VITE_API_BASE_URL` 指向 Go 实例；保留 Java 回滚副本直至观察期结束。
4. 回滚：将流量指回 Java；数据库若已由新版本迁移，需按迁移版本做逆向或从备份恢复（事先在变更窗口演练）。

## 已知差异（随实现迭代收敛）

- **Bedrock**：当前占位返回需接入 `aws-sdk-go-v2` 的 Converse 实现后可与 Java 对齐。
- **部分业务 API**：路由已按模块拆分，尚未全部挂载到 `Register`；以 Java `controller` 与前端 `api/index.ts` 为验收清单继续补全。
