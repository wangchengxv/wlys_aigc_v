# Tasks

## Phase 1: 数据库与配置层

- [ ] Task 1.1: 创建数据库迁移脚本 V23__social_account.sql
  - 新增 social_account 表（id, user_id, provider, provider_user_id, linked_at, INDEX）
  - 在 app_user 表增加 provider, provider_user_id 字段（可选，用于快速查询）

- [ ] Task 1.2: 扩展 AuthProperties 配置类
  - 增加 SocialProperties 配置类，包含多 provider 配置
  - 为每个 provider 支持 enabled, client-id, client-secret, redirect-uri 等配置

- [ ] Task 1.3: 在 application.yml 中增加社交登录配置示例

## Phase 2: 后端服务层

- [ ] Task 2.1: 创建 SocialAccount 实体类
  - 字段：id, userId, provider, providerUserId, linkedAt
  - Repository: SocialAccountRepository

- [ ] Task 2.2: 创建 SocialUserInfo DTO
  - 定义统一的用户信息结构（id, username, displayName, email, avatarUrl, provider）

- [ ] Task 2.3: 创建 SocialProvider 接口和 GitHub/Google/Wecom 实现类
  - 定义统一接口：getAuthUrl(), exchangeCodeForToken(), getUserInfo()
  - 实现各个 provider 的具体逻辑

- [ ] Task 2.4: 创建 SocialAuthService 服务类
  - 方法：buildAuthUrl(provider), handleCallback(provider, code), linkAccount(userId, provider, code)
  - 包含用户不存在时自动创建的逻辑

- [ ] Task 2.5: 创建 SocialAccountService 服务类
  - 方法：bindAccount(userId, provider, code), unbindAccount(userId, provider)
  - 解绑时检查是否还有其他登录方式

- [ ] Task 2.6: 扩展 AuthService
  - 可选：增加 socialLogin(provider, providerUserId) 方法作为备用

- [ ] Task 2.7: 扩展 AuthController
  - GET /auth/social/{provider} - 跳转授权页
  - GET /auth/social/callback/{provider} - 处理回调
  - POST /auth/social/unbind - 解绑账号
  - GET /auth/social/links - 获取已绑定列表

## Phase 3: 前端登录弹窗

- [ ] Task 3.1: 在 LoginModal 中增加社交登录按钮区域
  - 位置：登录表单下方，联调账号区块上方
  - 支持的 provider 图标（GitHub, Google, 企业微信）

- [ ] Task 3.2: 实现前端社交登录跳转逻辑
  - 点击按钮后调用后端授权跳转接口
  - 使用 window.location.href 跳转

- [ ] Task 3.3: 处理社交登录回调 URL
  - 在前端入口（如 App.tsx 或 router）增加回调处理逻辑
  - 解析回调参数，调用登录接口完成登录态建立

## Phase 4: 用户设置页

- [ ] Task 4.1: 创建「第三方账号绑定」UI 区块组件
  - 展示已绑定 provider 列表（带解绑按钮）
  - 展示可绑定但未绑定的 provider 列表（带绑定按钮）

- [ ] Task 4.2: 调用后端接口获取绑定列表和发起绑定/解绑
  - API: GET /api/v1/auth/social/links
  - API: POST /api/v1/auth/social/unbind

## Phase 5: 测试与文档

- [ ] Task 5.1: 后端单元测试
  - SocialAuthService 登录/注册逻辑测试
  - SocialAccountService 绑定/解绑逻辑测试

- [ ] Task 5.2: 手动验证流程
  - GitHub OAuth 完整流程测试
  - 解绑后用户仍能通过密码登录测试

## Task Dependencies

- Task 2.1 依赖 Task 1.1
- Task 2.2 依赖 Task 2.1
- Task 2.3 依赖 Task 2.2
- Task 2.4 依赖 Task 2.1, 2.2, 2.3
- Task 2.5 依赖 Task 2.1
- Task 2.6 依赖 Task 2.4
- Task 2.7 依赖 Task 2.4, 2.5
- Task 3.1 依赖 Task 2.7
- Task 3.2 依赖 Task 3.1
- Task 3.3 依赖 Task 2.7
- Task 4.1 依赖 Task 2.7
- Task 4.2 依赖 Task 4.1
- Task 5.2 依赖 Task 3.3, 4.2
