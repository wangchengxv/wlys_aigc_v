# 账号管理后台平台化升级 Spec

## Why
当前“组织与用户”页面已具备基础维护能力，但还不够接近传统 SpringBoot 云平台后台的账号治理深度（权限点、账号安全、批量导入导出、会话治理）。  
需要将学生/老师/管理员账号管理升级为“标准后台管理域”，统一前后端风格与能力边界，支持日常运营与审计追踪。

## What Changes
- 将账号管理模块升级为典型后台结构：筛选区 + 表格区 + 详情抽屉 + 批量工具栏 + 审计侧栏。
- 新增 RBAC 角色权限模型（角色、权限点、角色-权限绑定），并接入菜单与接口鉴权。
- 扩展账号生命周期：启用/停用、锁定/解锁、重置密码、登录失败计数、强制下线。
- 新增批量导入/导出、批量状态变更、批量角色调整能力。
- 增强审计：覆盖账号创建、编辑、权限变更、重置密码、锁定解锁、批量操作、会话操作。
- **BREAKING**：用户列表接口从“全量数组返回”调整为“分页+筛选参数+统计字段”返回结构。
- **BREAKING**：用户更新接口按操作类型拆分为多个子操作接口（资料更新、状态更新、密码重置、权限变更）。

## Impact
- Affected specs: `revamp-campus-aigc-admin-console`, `polish-campus-aigc-admin-ui`
- Affected code:
  - `aigc-site-react/src/pages/AdminDirectoryPage.tsx`
  - `aigc-site-react/src/api/index.ts`
  - `aigc-site-react/src/types/index.ts`
  - `aigc-server/src/main/java/com/example/aigc/controller/AdminDirectoryController.java`
  - `aigc-server/src/main/java/com/example/aigc/service/AdminDirectoryService.java`
  - `aigc-server/src/main/java/com/example/aigc/entity/AppUser.java`
  - `aigc-server/src/main/resources/db/migration/*`（新增账号治理相关迁移）

## ADDED Requirements
### Requirement: 平台化账号管理控制台
系统 SHALL 提供传统 SpringBoot 云平台风格的账号管理控制台，统一提供列表、筛选、详情、批量和审计入口。

#### Scenario: 管理员查看账号列表
- **WHEN** 管理员进入组织与用户管理页
- **THEN** 页面展示可组合筛选（关键字、角色、状态、锁定状态、组织、班级、时间范围）
- **THEN** 列表展示分页、排序、总数统计与当前筛选结果统计

#### Scenario: 管理员查看账号详情
- **WHEN** 管理员点击任意账号
- **THEN** 详情区展示基础信息、组织归属、权限信息、安全状态、最近登录和关键审计记录

### Requirement: RBAC 权限点模型
系统 SHALL 引入 RBAC 角色权限模型，支持角色绑定权限点，并将权限点用于菜单与接口控制。

#### Scenario: 角色权限生效
- **WHEN** 管理员为角色配置权限点并保存
- **THEN** 该角色用户登录后只可见被授权菜单，未授权接口返回无权限错误

#### Scenario: 权限变更可追踪
- **WHEN** 管理员调整角色权限或用户角色
- **THEN** 系统记录审计日志，包含操作者、变更前后摘要与时间

### Requirement: 账号安全与会话治理
系统 SHALL 支持账号锁定/解锁、重置密码策略、登录失败计数与会话强制下线。

#### Scenario: 登录失败自动锁定
- **WHEN** 同一账号连续登录失败达到阈值
- **THEN** 账号自动进入锁定状态并拒绝登录，直到管理员解锁或达到自动解锁条件

#### Scenario: 管理员强制下线
- **WHEN** 管理员执行“强制下线”
- **THEN** 目标账号现有登录态失效，需重新登录

### Requirement: 批量导入导出与批量操作
系统 SHALL 支持账号模板导入、结果回执、数据导出与批量操作。

#### Scenario: 批量导入账号
- **WHEN** 管理员上传模板文件
- **THEN** 系统返回成功/失败明细与失败原因，不因单条失败中断全部处理

#### Scenario: 批量状态更新
- **WHEN** 管理员勾选多条账号并执行启用/停用/锁定/解锁
- **THEN** 系统按条处理并返回汇总结果，界面展示成功与失败统计

## MODIFIED Requirements
### Requirement: 组织与用户管理页能力范围
现有“组织 / 班级 / 用户归属管理” SHALL 从基础目录维护升级为“账号治理中心”，并以服务端分页查询和操作型接口为主，不再依赖前端全量加载后本地筛选。

## REMOVED Requirements
### Requirement: 前端推断式安全状态
**Reason**: 当前页面通过更新时间近似“最近登录”、通过显示名推断“实名状态”，不满足后台治理准确性。  
**Migration**: 改为后端返回真实字段（最近登录时间、实名状态、锁定状态、失败次数等），前端仅展示不再推断。
