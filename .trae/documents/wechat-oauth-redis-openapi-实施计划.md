# 微信真实扫码登录 + Redis 状态机 + OpenAPI 实施计划

## Summary
- 目标：将当前占位版登录升级为可上线的真实链路，覆盖微信扫码登录全流程、验证码短信网关抽象、用户持久化（`user` 扩展字段）、OpenAPI 在线文档输出。
- 成功标准：
- 微信登录接口 `qrcode/poll/callback/bind` 可联调，配置存在时可完成真实登录，配置缺失时自动降级并给出明确错误。
- 手机验证码链路由内存实现切换为 Redis 状态机 + `SmsGateway` 抽象（先 Mock 实现）。
- 用户信息持久化到现有 `user` 表（新增 `phone/openid/nickname/avatar/update_time`），保留现有账号密码登录兼容。
- 后端输出 Springdoc 在线 OpenAPI 文档（Swagger UI + `/v3/api-docs`）。

## Current State Analysis
- 后端 `auth` 现状：
- `AuthController` 已有 `send-code/login/phone/wechat(qrcode/poll/bind占位)`，但微信接口返回 `unsupported`，不含真实 OAuth 回调处理。
- 验证码存储在 `LoginCodeService` 内存 Map，手机号用户映射在 `PhoneAuthUserService` 内存 Map。
- `JwtService` 已支持自定义过期时长，满足 autoLogin 长短 Token 场景。
- 数据层现状：
- Flyway 已有 `V1~V3`，`user` 表当前仅 `username/password_hash/created_at`。
- Redis 依赖已接入（`spring-boot-starter-data-redis`），但登录链路未实质使用。
- 前端现状：
- `LoginPage.tsx` 已有双 Tab 结构与轮询骨架，微信区域为占位文案，未显示真实二维码、未处理扫码状态机。
- 文档现状：
- `SPECS/api-contracts.md` 已记录手机号登录扩展与微信占位接口。
- 项目未引入 `springdoc-openapi`，当前无在线 OpenAPI 页面。

## Proposed Changes

### 1) 数据库与迁移
- 文件：
- `backend/src/main/resources/db/migration/V4__extend_user_for_phone_wechat.sql`（新增）
- 改动：
- 扩展 `user` 表字段：`phone`(唯一)、`openid`(唯一)、`nickname`、`avatar`、`update_time`。
- 增加索引/唯一约束：`uk_user_phone`、`uk_user_openid`。
- 原因：
- 满足“手机号登录 + 微信绑定 + 用户画像”持久化，且与当前架构保持单表兼容。
- 实现要点：
- 使用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 保障兼容。
- 保留 `username/password_hash`，不破坏现有 `POST /api/auth/login`。

### 2) 认证领域建模与仓储
- 文件：
- `backend/src/main/java/com/miioo/backend/auth/AuthUserEntity.java`（新增）
- `backend/src/main/java/com/miioo/backend/auth/AuthUserMapper.java`（新增）
- 改动：
- 用 MyBatis Plus 映射扩展后的 `user` 表。
- 原因：
- 替换当前手机号/微信用户内存映射，满足持久化与唯一绑定约束。
- 实现要点：
- 封装按 `phone/openid/id` 查询及创建/更新逻辑，处理唯一冲突并返回业务错误码。

### 3) 验证码能力切换到 Redis + 短信网关抽象
- 文件：
- `backend/src/main/java/com/miioo/backend/auth/LoginCodeService.java`（重构）
- `backend/src/main/java/com/miioo/backend/auth/SmsGateway.java`（新增）
- `backend/src/main/java/com/miioo/backend/auth/MockSmsGateway.java`（新增）
- `backend/src/main/resources/application.yml`（新增短信/验证码配置）
- 改动：
- `LoginCodeService` 使用 Redis 实现验证码与防刷键：
- `login:code:{phone}`（5分钟）
- `login:code:rate:{phone}`（60秒）
- 新增 `SmsGateway` 抽象，当前注入 `MockSmsGateway`（日志输出验证码；测试环境可返回 debugCode）。
- 原因：
- 满足上线可扩展要求，避免直接耦合某家短信厂商。
- 实现要点：
- 发送验证码：原子写 rate key + code key，命中 rate key 直接报 429。
- 校验验证码：一致性校验后立即删除 code key（一次性消费）。

### 4) 微信 OAuth2 全链路（后端）
- 文件：
- `backend/src/main/java/com/miioo/backend/auth/AuthController.java`（重构接口实现）
- `backend/src/main/java/com/miioo/backend/auth/WechatAuthService.java`（新增）
- `backend/src/main/java/com/miioo/backend/auth/WechatClient.java`（新增，调用微信接口）
- `backend/src/main/java/com/miioo/backend/auth/WechatProperties.java`（新增配置类）
- `backend/src/main/resources/application.yml`（新增 `miioo.auth.wechat.*`）
- 改动：
- 实现真实接口：
- `GET /api/auth/wechat/qrcode`：生成 `state + pollId`，写 Redis，返回二维码 URL 与 `pollId`。
- `GET /api/auth/wechat/poll?id=...`：读取 Redis 状态，返回 `pending/need_bind/success`。
- `GET /api/auth/wechat/callback`：校验 `state`，用 `code` 换取 `openid`，更新 poll 状态。
- `POST /api/auth/wechat/bind`：验证码校验 + 手机号绑定 + 生成 JWT。
- 配置缺失自动降级：
- 当 `appid/secret/redirect-uri` 缺失时，`qrcode` 返回业务错误（例如 code=501,message=未配置），不影响手机号登录。
- 原因：
- 满足用户选择的“后端全链路+前端联调”与“配置缺失自动降级”。
- Redis 状态机约定：
- `login:wechat:state:{state}` -> `pollId`（10分钟）
- `login:wechat:poll:{pollId}` -> Hash(`status`,`openid`,`token`,`userJson`)（10分钟）
- 状态流转：`pending -> need_bind -> success`，超时后自动过期。

### 5) 前端微信登录真实联调
- 文件：
- `frontend/src/api/authApi.ts`（扩展类型与接口）
- `frontend/src/pages/LoginPage.tsx`（改造微信 Tab）
- 改动：
- 微信 Tab 显示真实二维码（后端返回 URL），每 2s 轮询 `poll`。
- 状态处理：
- `pending`：提示“请扫码确认”
- `need_bind`：弹出手机号验证码绑定表单（复用验证码发送能力）
- `success`：写入 token，更新 user store，跳转 `/projects`
- 失败与超时：友好提示 + 提供“刷新二维码”按钮。
- 原因：
- 对齐需求文档的微信扫码主流程，形成可验证的前后端闭环。

### 6) OpenAPI 在线文档（Springdoc）
- 文件：
- `backend/pom.xml`（新增 springdoc 依赖）
- `backend/src/main/java/com/miioo/backend/config/OpenApiConfig.java`（新增，可选）
- `backend/src/main/java/com/miioo/backend/auth/AuthController.java`（补充接口注解）
- `SPECS/api-contracts.md`（同步 URL 与字段）
- 改动：
- 引入 `springdoc-openapi-starter-webmvc-ui`。
- 标准访问路径：`/swagger-ui/index.html`、`/v3/api-docs`。
- 为 auth 核心接口补充请求/响应示例与错误码说明。
- 原因：
- 满足“Springdoc在线文档”交付要求，便于前后端并行开发。

### 7) 安全与兼容策略
- 文件：
- `backend/src/main/java/com/miioo/backend/config/SecurityConfig.java`（按需补白名单）
- `backend/src/main/java/com/miioo/backend/common/GlobalExceptionHandler.java`（按需统一错误码映射）
- 改动：
- 放行微信回调与 OpenAPI 路径（仅必要接口）。
- 统一关键错误码：`400` 参数错误、`401` 未认证、`429` 防刷、`501` 未配置。
- 兼容性：
- 保留既有 `POST /api/auth/login`、`GET /api/auth/me` 行为，不做破坏式修改。

## 接口与数据流（决策版）
- 手机号登录流：
- `send-code(phone)` -> Redis 存 code/rate -> `SmsGateway.send()` -> `login/phone(phone,code,autoLogin)` -> 校验并消费 code -> upsert 用户 -> 颁发 JWT。
- 微信扫码流：
- `wechat/qrcode` -> 生成 `state/pollId` + Redis 标记 `pending` -> 前端展示二维码并轮询 `poll`。
- 微信扫码后微信回调 `wechat/callback(code,state)` -> 换取 openid -> 更新 poll 状态为 `need_bind` 或直接 `success`（若 openid 已绑定手机号）。
- 前端收到 `need_bind` -> `wechat/bind(openid,phone,code)` -> 校验验证码并完成绑定 -> 返回 token/userInfo。

## Assumptions & Decisions
- 已确认决策：
- 微信接入深度：后端全链路 + 前端联调。
- 短信方案：`SmsGateway` 抽象 + Mock 实现（本期不绑定云厂商）。
- OpenAPI 输出：Springdoc 在线文档。
- 用户数据策略：扩展现有 `user` 表，不新建认证表。
- 发布策略：微信配置缺失自动降级，不影响手机号登录主链路。
- 假设前提：
- 微信公众号参数（`appid/secret/redirect-uri`）可通过环境变量注入。
- Redis 服务可用，允许缓存验证码和微信轮询状态。

## Testing & Acceptance Criteria
- 后端测试：
- 新增 `PhoneLoginIntegrationTest` 扩展用例：验证码防刷、验证码一次性消费。
- 新增 `WechatLoginIntegrationTest`：`qrcode -> callback -> poll -> bind/success` 主流程与异常分支。
- 保持 `ProjectSecurityIntegrationTest` 通过，确保鉴权回归不破坏。
- 前端验证：
- 微信 Tab：二维码加载、轮询状态更新、need_bind 弹窗绑定成功、异常提示路径。
- 手机验证码：倒计时、防刷提示、autoLogin 存储分流（local/session）。
- 验收口径：
- 配置完整时可完成真实扫码登录。
- 配置缺失时接口返回明确未配置错误，页面可回退手机号登录。
- Swagger UI 可访问且包含 auth 新增接口。

## Verification Steps
- 后端：
- `mvn test`
- 手工联调：
- 访问 `/swagger-ui/index.html` 查看接口与示例。
- 调用 `GET /api/auth/wechat/qrcode`，确认返回二维码与 `pollId`。
- 模拟/真实回调 `GET /api/auth/wechat/callback` 后，`poll` 状态正确流转。
- 前端：
- `npm run build`
- 启动后验证登录页双链路：手机号验证码与微信扫码都可走通（按环境能力）。

## Rollout & Monitoring
- 灰度策略：
- 先在 dev/test 开启微信参数联调，prod 缺失参数保持自动降级。
- 监控建议：
- 记录关键日志：`send-code` 频控命中、微信回调 state 校验失败、bind 冲突。
- 指标建议：验证码发送成功率、微信扫码成功率、绑定失败率、429 命中率。
