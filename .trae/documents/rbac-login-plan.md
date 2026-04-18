## Summary

- 目标：基于 `execution-checklist.md` 中尚未完成的“正式账号登录体系 + RBAC 角色模型”补齐第一版实现。
- 范围：采用账号密码登录、JWT Bearer 鉴权、配置文件种子管理员、角色与资源归属结合的权限判断。
- 前端交互：新增独立登录页；设置页展示当前账号信息并支持退出。
- 特殊规则：教师可查看与自己教学任务相关的学生提交并进行评分/点评，但不能删除学生项目。

## Current State Analysis

- 后端当前通过 `RequestAuthService` 校验固定 `access-token`，并从 `x-user-id / x-user-name / x-org-unit-id / x-course-id` 请求头拼出 `RequestUserContext`。
- 课程、作业、提交、评分链路已存在，权限主要依赖“创建者本人”和 `orgUnitId/courseId` 的开发态可见性判断。
- `script-projects` 已通过 `ScriptProjectAccessInterceptor` 做“项目 owner 本人”访问拦截。
- 前端 `aigc-site-react/src/api/index.ts` 会自动注入开发态 token 与伪造用户头，尚无正式登录页、令牌存储和当前用户上下文。
- 数据库现有教学表迁移在 `V14__teaching_flow_baseline.sql`，JPA 仓储集中声明在 `repository/jpa/SpringDataRepositories.java`。

## Proposed Changes

### 后端数据与配置

- `aigc-server/src/main/resources/db/migration/V15__auth_and_rbac.sql`
  - 新增用户表、密码哈希字段、角色字段、启用状态、组织/班级归属字段。
  - 新增课程成员/班级成员关系表，支持教师-课程、学生-课程归属。
- `aigc-server/src/main/resources/application.yml`
  - 新增 JWT 密钥、过期时间、种子管理员账号配置。
- `aigc-server/src/main/java/com/example/aigc/config/AuthProperties.java`
  - 从“固定 access-token”扩展为兼容 JWT、种子管理员、开发兜底配置。

### 后端认证与上下文

- 新增实体/枚举/DTO：
  - `entity/AppUser.java`
  - `entity/CourseMembership.java`
  - `enums/UserRole.java`
  - `dto/LoginRequest.java`
  - `dto/LoginResponse.java`
  - `dto/CurrentUserResponse.java`
- 新增仓储与服务：
  - `repository/AppUserRepository.java`
  - `repository/CourseMembershipRepository.java`
  - `repository/jpa/JpaAppUserRepository.java`
  - `repository/jpa/JpaCourseMembershipRepository.java`
  - `service/AuthService.java`
  - `service/UserBootstrapService.java`
  - `service/PasswordCodec.java`
  - `service/JwtTokenService.java`
- 扩展 `RequestUserContext.java`
  - 增加 `role`、`authenticated` 等字段，保留 `userId/userName/orgUnitId/courseId`。
- 重构 `RequestAuthService.java`
  - 支持 `/api/v1/auth/login` 免登录访问。
  - 解析 JWT，构造正式用户上下文。
  - 在必要时保留开发态访问头兼容能力，避免一次性打断现有非教学接口。
- 新增/调整拦截器与控制器：
  - 新增 `AuthController.java`，提供登录、当前用户、退出占位接口。
  - 调整 `WebConfig.java`，为受保护接口挂载统一鉴权拦截。
  - 调整 `ScriptProjectAccessInterceptor.java`，从“仅 owner 可访问”变为调用权限服务判断读/写/删除。

### 后端权限模型

- 新增 `service/AuthorizationService.java`
  - 封装角色级规则：
    - `ADMIN` 可管理全局。
    - `TEACHER` 可创建/管理课程、作业、评分。
    - `STUDENT` 可查看本人课程、提交本人项目。
  - 封装资源归属规则：
    - 课程：创建者、课程教师、课程学生、管理员。
    - 作业：发布者、课程教师、课程学生、管理员。
    - 提交：学生本人可查看；教师可评分/点评；管理员可查看。
    - 项目：owner 可读写删；相关教师可只读查看用于评分，不可删除。
- 调整服务层：
  - `TeachingCourseService.java`
  - `TeachingAssignmentService.java`
  - `TeachingSubmissionService.java`
  - `ScriptProjectService.java`
  - `AuditLogController.java`
  - `StyleTemplateService.java`
  - 用统一权限服务替代零散 owner 判断。

### 前端登录与权限接入

- `aigc-site-react/src/types/index.ts`
  - 新增登录、当前用户、角色相关类型。
- `aigc-site-react/src/api/index.ts`
  - 新增登录、获取当前用户、退出接口。
  - 用 JWT 令牌替代开发态固定 token 注入。
  - 仅在开发兜底模式下保留原伪用户逻辑。
- 新增状态管理：
  - `src/stores/authStore.ts`
  - 负责 token、本地持久化、用户信息拉取、登录/退出。
- 新增页面/组件：
  - `src/pages/LoginPage.tsx`
  - 必要时新增轻量鉴权守卫组件。
- 调整：
  - `src/router.tsx` 增加 `/login` 与受保护路由逻辑。
  - `src/layouts/AppLayout.tsx` 启动时拉取当前用户。
  - `src/components/layout/TopNav.tsx` 展示当前角色/用户信息与退出入口。
  - `src/pages/SettingsPage.tsx` 增加当前账号、角色说明、退出入口。
  - 教学相关页面按角色控制操作按钮显隐。

### 测试与验证

- 扩展 `aigc-server/src/test/java/com/example/aigc/TeachingWorkflowIntegrationTest.java`
  - 覆盖教师登录后发布作业、学生登录后提交、教师评分。
  - 覆盖学生不能评分、不能归档课程。
  - 覆盖教师可查看学生提交用于评分但不能删除学生项目。
- 新增认证测试：
  - 登录成功/失败。
  - 种子管理员可登录。

## Assumptions & Decisions

- 第一版密码采用 Spring 自带安全哈希方案，不做注册开放，仅由种子管理员和后续后台建人。
- 第一版先落地课程成员关系，不在本次同时完成完整“组织/班级管理 UI”。
- 为避免大面积回归，非教学工作流接口优先兼容现有开发模式；教学主链路优先切到正式登录态。
- 退出接口前端以清理本地 JWT 为主，服务端不做 token 黑名单。

## Verification Steps

- 后端测试：运行教学集成测试与新增认证测试，确认登录、课程、作业、提交、评分、只读访问规则通过。
- 手动验证：
  - 管理员登录成功并可进入系统。
  - 教师可创建课程、发布作业、评分、查看相关学生项目但不可删除。
  - 学生可查看自己课程、提交本人项目，但不能评分或归档课程。
  - 未登录访问受保护页面会跳转 `/login`。
