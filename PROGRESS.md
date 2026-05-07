# PROGRESS - 项目记忆

## 当前状态
- 日期：2026-05-07
- 阶段：一期闭环工程化落地（持久化与权限主链路已落地）
- 基线文档：`Miioo动画视频创作平台-详细开发文档.md`

## 已完成
- 建立 `AGENTS.md`（协作规矩与执行流程）。
- 建立 `ARCHITECTURE.md`（系统地图与模块边界）。
- 建立 `SPECS/`（范围、功能、非功能、安全、接口契约）。
- 建立 `RULES.md`（强制红线）。
- 建立 `TESTS.md`（质量底线与最小用例）。
- 建立 `SKILLS/` 与 `DOCS/` 初始知识体系。
- 建立前后端同仓骨架：`frontend/`、`backend/`、`docker-compose.yml`、根目录运行说明。
- 完成后端一期基础实现：`auth/project/script/subject/asset/aitask/model` 接口骨架。
- 完成 AI Mock 适配与异步任务最小链路：任务创建、轮询查询、失败/取消/重试。
- 完成数据库迁移脚本：`V1__init_core_tables.sql`、`V2__add_indexes_and_constraints.sql`。
- 完成前端一期页面骨架：登录、项目列表、项目工作台（全局/剧本/主体/资产）。
- 完成基础构建验证：`backend mvn -DskipTests compile`、`frontend npm run build` 通过。
- 完成后端一期核心持久化替换：`project/script/subject/asset/aitask` 从 InMemory 切换到 MyBatis Plus。
- 完成项目归属校验强化：项目、脚本、主体、资产、任务接口统一按当前用户校验访问权限。
- 完成任务归属字段补充：新增 Flyway 脚本 `V3__add_user_id_to_ai_task.sql`。
- 完成最小自动化测试补齐：新增 `ProjectSecurityIntegrationTest`，覆盖未登录 401 与跨用户越权拦截。
- 完成历史代码清理：删除未使用 `InMemoryDb`，避免实现口径分裂。
- 完成 Service 化第一步收敛：新增 `ProjectService`、`ScriptService`、`SubjectService`，控制器仅保留参数接收与响应封装。
- 完成登录模块一期增强：新增手机号验证码发送与登录接口，支持 60 秒防刷、验证码一次性消费、自动登录时效区分（短时/7天）。
- 完成前端登录页升级：接入“验证码登录 + 微信登录（占位）”双 Tab，支持验证码倒计时、自动登录存储策略、路由鉴权守卫。
- 完成认证契约更新：`SPECS/api-contracts.md` 补充 `send-code`、`login/phone` 与微信占位接口说明。
- 新增登录回归测试：`PhoneLoginIntegrationTest` 覆盖验证码登录成功与验证码错误场景。

## 下一步建议
- 按 `TESTS.md` 继续补齐自动化测试（重点：任务状态机、失败重试、资源删除约束）。
- 推进 Service 化第二步：补齐 `asset/aitask` 的一致风格改造与事务边界审视。
- 推进剧本分集、主体引用检查、资产筛选与分页等一期细节能力。
- 补齐微信登录真实联调（公众号配置、回调鉴权、轮询状态持久化与手机号绑定）。

## 决策记录
- 决策：以详细开发文档为单一事实源（SSOT）构建 Harness。
- 原因：减少口径分裂和跨文档冲突。
- 影响：所有新增需求都需先过 `SPECS` 再进入实现。

## 风险与阻塞
- 风险：图片生成链路使用 Mock Provider，尚未联调真实模型厂商。
- 风险：当前接口仍以 Controller 直连 Mapper 为主，复杂事务与幂等边界需在 Service 层继续收敛。
- 风险：验证码暂以内存存储用于开发联调，生产环境需切换 Redis 并接入短信网关。
- 建议：下一轮优先推进 Service 化重构与任务失败重试策略细化。

## 变更日志模板
- 日期：
- 变更模块：
- 变更摘要：
- 对应规格：
- 测试结论：
- 未决事项：
