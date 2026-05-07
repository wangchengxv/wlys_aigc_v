# API Contracts

## 1. 通用响应
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "traceId": "string"
}
```

## 2. 通用分页
```json
{
  "pageNo": 1,
  "pageSize": 20,
  "total": 0,
  "records": []
}
```

## 3. 关键接口分组
- 认证：`POST /api/auth/login`、`GET /api/auth/me`、`GET /api/auth/send-code`、`POST /api/auth/login/phone`
- 微信登录（占位）：`GET /api/auth/wechat/qrcode`、`GET /api/auth/wechat/poll`、`POST /api/auth/wechat/bind`
- 项目：`/api/projects`（CRUD + summary）
- 剧本：`/api/scripts/generate`、`/upload`、`/extract-subjects`、`/api/scripts`
- 主体：`/api/subjects`（CRUD + generate + finalize）
- 分镜：`/api/storyboards`（init/list/update/reorder/generate/finalize）
- 渲染：`/api/render/preview`、`/export`、`/tasks/*`
- 资产：`/api/assets`（upload/list/detail/delete/star/batch-download/selectable）+ `POST /api/assets/generate-image`
- 任务：`/api/ai-tasks/*`（detail/progress/cancel/retry/my）

## 4. 状态约束
- AI 任务状态：`PENDING` -> `RUNNING` -> `SUCCESS|FAILED|CANCELLED`。
- 分镜状态：`DRAFT`、`IMAGE_DONE`、`VIDEO_DONE`、`FINALIZED`。
- 所有涉及项目上下文接口必须验证当前用户归属权限。

## 5. 契约变更规范
- 变更前先修改本文件并标记“兼容性影响”。
- 非兼容变更必须提供迁移策略和回滚策略。
- 字段新增优先向后兼容，禁止无通知删除字段。

## 6. 认证扩展契约（2026-05-07）
- `GET /api/auth/send-code?phone=13800138000`
- 响应：`{ code, message, data: { resendAfterSeconds } }`；测试环境可返回 `debugCode`。
- 约束：手机号格式校验、60 秒防刷、验证码 5 分钟有效。
- `POST /api/auth/login/phone`
- 请求：`{ "phone":"13800138000", "code":"123456", "autoLogin":true }`
- 响应：`{ code, message, data: { token, userInfo } }`
- 约束：验证码一次性消费；`autoLogin=true` 使用长时效 Token，否则短时效 Token。
- 兼容性影响：新增接口与新增响应字段，属于向后兼容变更，不影响现有 `/api/auth/login`。
