# 前端渐进式收纳交互 Spec

## Why
当前前端在同一屏内同时暴露了较多信息卡片、说明区、操作区和结果区，用户进入页面后容易感到拥挤和凌乱。需要把界面调整为“默认简洁、按需展开”的使用方式，让功能可达但不过度占据首屏注意力。

## What Changes
- 建立“默认只展示核心信息，次级信息通过点击展开”的前端交互原则
- 为低频功能、补充说明和高级配置提供固定栏或固定行的触发占位，避免按钮和面板四处散落
- 收敛后台壳层、概览页、工作台、项目详情页等高信息密度页面的同屏模块数量
- 统一展开行为、收起行为、展开区域位置和状态反馈，减少页面跳动与认知切换成本
- 保留现有功能能力，但通过按需展开、分层切换、单焦点查看的方式降低界面噪音

## Impact
- Affected specs: `revamp-campus-aigc-admin-console`, `polish-campus-aigc-admin-ui`, `enhance-campus-delivery-showcase-ui`
- Affected code: `aigc-site-react/src/layouts/AppLayout.tsx`、`aigc-site-react/src/components/layout/TopNav.tsx`、`aigc-site-react/src/pages/OperationsDashboardPage.tsx`、`aigc-site-react/src/pages/WorkspacePage.tsx`、`aigc-site-react/src/pages/ScriptProjectDetailPage.tsx` 及相关共享面板与样式系统

## ADDED Requirements
### Requirement: 渐进式信息暴露
系统 SHALL 在页面默认状态下仅展示当前任务所需的核心内容，把补充说明、低频操作、高级配置和详情信息收纳到显式可点击的展开入口中。

#### Scenario: 首屏默认简洁
- **WHEN** 用户首次进入概览页、工作台或项目详情页
- **THEN** 页面首屏只展示核心摘要、主操作和当前状态
- **AND** 次级模块不应默认全部展开

#### Scenario: 点击后再展开功能区
- **WHEN** 用户点击固定栏或固定行中的功能按钮
- **THEN** 对应功能区在预设位置展开
- **AND** 用户可以明确知道当前展开的是哪个功能区

### Requirement: 固定触发占位
系统 SHALL 为可展开功能提供稳定的触发入口位置，按钮需要集中放置在固定栏、固定行或统一操作区中，而不是分散插入多个信息块内部。

#### Scenario: 按钮位置稳定
- **WHEN** 页面包含多个可展开功能
- **THEN** 这些功能的触发按钮应在同一视觉分组内呈现
- **AND** 页面在展开前后仍保持主要布局稳定

### Requirement: 单焦点展开体验
系统 SHALL 优先采用单焦点展开策略，使同一组互斥信息区在任一时刻只突出一个活动面板，除非页面职责明确要求并行对照。

#### Scenario: 同组功能切换
- **WHEN** 用户在同一组触发按钮之间切换
- **THEN** 当前活动面板切换为新的目标面板
- **AND** 非活动面板收起或弱化显示

## MODIFIED Requirements
### Requirement: 后台页面信息结构
现有后台页面 SHALL 从“多窗口并排暴露”调整为“摘要常驻 + 固定触发入口 + 按需展开详情”的结构。导航、页头、概览卡、详情说明、快捷入口和工作台面板都应按信息优先级重新排序，优先保证首屏简洁、主路径清晰、低频信息可回找。

#### Scenario: 高信息密度页面改造
- **WHEN** 页面同时包含摘要、统计、操作、说明、历史记录和配置内容
- **THEN** 系统应默认展示摘要和主操作
- **AND** 其余内容通过折叠面板、切换面板或抽屉式区域分层打开

## REMOVED Requirements
- 无
