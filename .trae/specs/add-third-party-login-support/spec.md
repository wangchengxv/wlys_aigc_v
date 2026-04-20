# 第三方登录支持方案

## Why

当前项目仅支持基于用户名/密码的传统登录方式。为提升用户体验、降低注册门槛，需要增加第三方登录能力（如 GitHub、Google、企业微信等 OAuth 2.0 / OIDC 登录），同时保持现有账号密码登录体系的向后兼容。

## What Changes

### 后端扩展

1. **数据库层面**
   - 在 `app_user` 表增加 `provider`、`provider_user_id`、`linked_at` 字段，用于记录第三方登录关联
   - 新增 `social_account` 关系表，支持同一用户绑定多个第三方账号

2. **配置层面**
   - 在 `AuthProperties` 中增加 OAuth2 客户端配置（client-id、client-secret、redirect-uri 等）
   - 支持多provider配置（GitHub、Google、企业微信等）

3. **服务层面**
   - 新增 `SocialAuthService`：处理 OAuth2 授权回调、用户关联/创建逻辑
   - 扩展 `AuthService.login()`：支持 social token 换发 JWT
   - 新增 `SocialAccountService`：管理用户的第三方账号绑定与解绑

4. **控制器层面**
   - `AuthController` 增加 `/auth/social/{provider}` 授权跳转接口
   - `AuthController` 增加 `/auth/social/callback/{provider}` 回调接收接口
   - `AuthController` 增加 `/auth/social/unbind` 解绑接口

### 前端扩展

1. **登录弹窗**
   - 在 `LoginModal` 中增加第三方登录按钮区域
   - 每个 provider 独立按钮，点击后跳转后端授权接口

2. **状态管理**
   - `authStore` 支持处理第三方登录回调后的用户信息更新

3. **用户设置页**
   - 在 `SettingsPage` 增加「第三方账号绑定」区块，展示已绑定/可绑定 provider

## Impact

- **影响范围**：
  - 后端：`aigc-server/src/main/java/com/example/aigc/` 下的 auth 相关模块
  - 前端：`aigc-site-react/src/components/common/LoginModal.tsx`、`aigc-site-react/src/pages/SettingsPage.tsx`
  - 数据库：新增 migration 脚本

- **向后兼容**：现有用户名/密码登录保持不变，作为默认兜底

## ADDED Requirements

### Requirement: OAuth2 授权跳转

系统 SHALL 提供第三方登录授权跳转接口。

#### Scenario: 用户点击 GitHub 登录
- **WHEN** 用户在登录弹窗点击 GitHub 登录按钮
- **THEN** 前端跳转至后端 `/api/v1/auth/social/github` 接口
- **AND** 后端重定向至 GitHub OAuth 授权页面

### Requirement: OAuth2 回调处理

系统 SHALL 处理 OAuth2 授权回调并完成登录/注册。

#### Scenario: GitHub 授权成功，老用户首次使用社交登录
- **WHEN** GitHub 返回授权码并回调至 `/api/v1/auth/social/callback/github`
- **AND** 系统中已存在相同 GitHub user_id 的绑定记录
- **THEN** 系统完成登录并返回 JWT token

#### Scenario: GitHub 授权成功，新用户首次使用社交登录
- **WHEN** GitHub 返回授权码并回调至 `/api/v1/auth/social/callback/github`
- **AND** 系统中不存在该 GitHub user_id 的绑定记录
- **THEN** 系统自动创建新用户（username 格式：`github_{github_user_id}`）并完成登录

### Requirement: 第三方账号绑定管理

系统 SHALL 支持已登录用户绑定和解绑第三方账号。

#### Scenario: 已登录用户绑定第三方账号
- **WHEN** 已登录用户在设置页点击绑定 GitHub 账号
- **THEN** 系统跳转至 GitHub 授权页面
- **AND** 授权成功后，用户的 app_user 记录关联该 provider_user_id

#### Scenario: 已登录用户解绑第三方账号
- **WHEN** 已登录用户在设置页点击解绑某第三方账号
- **THEN** 系统解除该第三方账号与用户的关联
- **AND** 若用户无密码则提示设置密码（防止无法登录）

## MODIFIED Requirements

### Requirement: 现有登录流程

[保持不变，LoginModal 和 AuthService.login() 的用户名密码流程继续支持]

## REMOVED Requirements

无

## 技术方案选型

### 推荐方案：Spring Security OAuth2 Authorization Server + 简单自实现

考虑到项目已有 Spring Boot 基础，推荐采用：

1. **轻量方案（推荐）**：使用 Spring Boot OAuth2 Client 依赖，手动处理授权回调，不引入完整的 Authorization Server
2. **完整方案**：引入 Spring Authorization Server，提供完整的 OAuth2/OIDC 服务

**推荐轻量方案**，原因：
- 项目规模适中，无需完整的 OAuth2 授权服务器功能
- 减少依赖复杂度，降低维护成本
- 灵活性更高，可自定义用户体验

### 支持的 Provider

| Provider | 授权 URL | Token 交换 | 用户信息 API |
|----------|----------|------------|--------------|
| GitHub | https://github.com/login/oauth/authorize | https://github.com/login/oauth/access_token | https://api.github.com/user |
| Google | https://accounts.google.com/o/oauth2/v2/auth | https://oauth2.googleapis.com/token | https://www.googleapis.com/oauth2/v2/userinfo |
| 企业微信 | https://open.work.weixin.qq.com/wwopen/sso/3rd_qrconnect | https://qyapi.weixin.qq.com/cgi-bin/gettoken | https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo |

## 配置示例

```yaml
aigc:
  auth:
    # 现有配置保持不变
    jwt-secret: ${AIGC_JWT_SECRET:...}
    jwt-expire-minutes: ${AIGC_JWT_EXPIRE_MINUTES:720}
    
    # 新增 OAuth2 客户端配置
    social:
      github:
        enabled: true
        client-id: ${GITHUB_CLIENT_ID:}
        client-secret: ${GITHUB_CLIENT_SECRET:}
        redirect-uri: ${AIGC_BASE_URL}/api/v1/auth/social/callback/github
      google:
        enabled: false
        client-id: ${GOOGLE_CLIENT_ID:}
        client-secret: ${GOOGLE_CLIENT_SECRET:}
        redirect-uri: ${AIGC_BASE_URL}/api/v1/auth/social/callback/google
      wecom:
        enabled: false
        client-id: ${WECOM_CLIENT_ID:}
        client-secret: ${WECOM_CLIENT_SECRET:}
        redirect-uri: ${AIGC_BASE_URL}/api/v1/auth/social/callback/wecom
        agent-id: ${WECOM_AGENT_ID:}
```
