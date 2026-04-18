# 前端改版收尾验收补齐 Spec

## Why
当前改版主干已基本完成，但仍缺少验收与交付文档沉淀，以及部分“帮助说明统一形态”的落地核对。需要完成最后一轮收口，确保可交付、可追溯、可复验。

## What Changes
- 新增并补齐改版验收清单文档，覆盖路由、角色、视口与截图记录
- 新增并补齐交付说明文档，沉淀改版范围、复用组件、去重入口与扩展建议
- 对固定回归路由执行一次人工验收并记录结果
- 对 ADMIN / TEACHER / STUDENT 角色执行一次入口可见性与管理员页拦截验收并记录结果
- 对 1440 / 1024 / 768 / 390 四档响应式执行验收并记录结果
- 将项目链路内帮助说明统一为一致形态（优先使用 HelpHint；若页面不适用，则记录替代方案与原因）

## Impact
- Affected specs: `revamp-campus-aigc-admin-console`, `simplify-progressive-disclosure-ui`, `remove-hover-hints-and-rebalance-layout`
- Affected code: `aigc-site-react/docs/`, `aigc-site-react/src/pages/`, `aigc-site-react/src/components/common/HelpHint.tsx` 相关使用点

## ADDED Requirements
### Requirement: 改版交付文档完整性
系统 SHALL 提供可版本化的改版验收清单与交付说明文档，支持后续复验与扩展。

#### Scenario: 文档可追溯
- **WHEN** 团队查看改版收尾结果
- **THEN** 能在 docs 中直接看到路由、角色、视口、截图占位与通过情况，以及交付摘要与后续建议

## MODIFIED Requirements
### Requirement: 收尾验收基线
系统 SHALL 在当前代码基线下完成 lint/build 与人工验收记录，并确保核心入口、角色边界、响应式布局满足既定标准。

#### Scenario: 回归验收通过
- **WHEN** 执行固定回归页面、角色边界与响应式核查
- **THEN** 验收文档中对应项被勾选并附简要结论，不再仅依赖口头确认

### Requirement: 项目链路帮助说明形态一致
系统 SHALL 在项目子页链路中统一帮助说明表现形式，避免同类说明在不同页面体验割裂。

#### Scenario: 帮助说明一致
- **WHEN** 用户访问项目次级页
- **THEN** 页面帮助说明要么统一使用 HelpHint，要么在文档中声明替代模式与原因

## REMOVED Requirements
### Requirement: 仅依赖临时口头验收
**Reason**: 口头结论不可追溯，无法在后续迭代中高效复验。  
**Migration**: 将验收结果固化到 docs 文档中，配合 lint/build 与页面核查形成可复用交付基线。
