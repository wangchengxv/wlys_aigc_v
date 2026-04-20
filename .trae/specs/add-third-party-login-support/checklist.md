# Checklist

## 数据库与配置

- [ ] social_account 表已创建，包含正确的字段和索引
- [ ] AuthProperties 已扩展支持 social 配置
- [ ] application.yml 包含社交登录配置示例

## 后端服务层

- [ ] SocialAccount 实体类和 SocialAccountRepository 已创建
- [ ] SocialProvider 接口已定义，GitHub/Google/Wecom 实现类已创建
- [ ] SocialAuthService 包含正确的用户创建和登录逻辑
- [ ] SocialAccountService 包含绑定和解绑逻辑，解绑时检查是否有其他登录方式
- [ ] AuthController 包含所有新接口：
  - GET /auth/social/{provider} 跳转授权
  - GET /auth/social/callback/{provider} 处理回调
  - POST /auth/social/unbind 解绑
  - GET /auth/social/links 获取绑定列表

## 前端登录弹窗

- [ ] LoginModal 包含第三方登录按钮区域
- [ ] 按钮点击后正确跳转至后端授权接口
- [ ] 前端正确处理回调 URL 并建立登录态

## 用户设置页

- [ ] SettingsPage 包含「第三方账号绑定」区块
- [ ] 已绑定 provider 显示解绑按钮
- [ ] 未绑定 provider 显示绑定按钮
- [ ] 解绑时检查用户是否仍有密码登录能力

## 安全性

- [ ] OAuth2 state 参数正确使用，防止 CSRF
- [ ] client_secret 在配置中使用环境变量，未硬编码
- [ ] 解绑操作要求用户身份验证

## 向后兼容

- [ ] 现有用户名/密码登录流程不受影响
- [ ] 已有的 seed users 仍可正常登录
