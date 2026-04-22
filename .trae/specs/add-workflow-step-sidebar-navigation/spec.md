# 工作流程区侧边栏步骤导航 Spec

## Why
当前剧本工程项目的工作流程（总览、剧本、资产、视频、配音、口型、剪辑、导出）以顶部 Tab 列表形式展示，用户无法直观看到整个流程的进度和当前位置。缺少流程指引会导致用户不清楚下一步该做什么，也不知道当前步骤在整个生产链路中的位置。

## What Changes
- 将工作流程区从顶部横向 Tab 布局改为左侧垂直侧边栏布局
- 每个步骤显示编号、名称和状态（待处理/进行中/已完成）
- 当前步骤高亮显示，流程推动时自动点亮下一步
- 支持用户点击侧边栏快速跳转
- 添加下一步引导提示
- **BREAKING**: 布局从顶部 Tab 改为左侧侧边栏，CSS 样式需全面调整

## Impact
- Affected specs: 剧本工程项目工作区
- Affected code: `aigc-site-react/src/components/script/ScriptProjectWorkflowNav.tsx`、`aigc-site-react/src/styles/global.css`

## ADDED Requirements
### Requirement: 工作流程侧边栏必须展示流程步骤
系统 SHALL 在项目工作区左侧展示垂直侧边栏，包含所有流程步骤的编号和名称。

#### Scenario: 用户进入项目工作区
- **WHEN** 用户访问任何项目子页面（总览、剧本、资产等）
- **THEN** 左侧侧边栏始终显示完整的流程步骤列表
- **AND** 每个步骤带有序号（1-8）
- **AND** 当前页面所对应的步骤被高亮

### Requirement: 流程步骤必须显示状态
系统 SHALL 为每个步骤显示状态：未开始（灰色）、进行中（主色高亮）、已完成（完成标记）。

#### Scenario: 用户完成了剧本阶段
- **WHEN** 用户在剧本页面完成剧本完善
- **THEN** 剧本步骤标记为已完成
- **AND** 下一个步骤（资产）自动变为进行中
- **AND** 侧边栏显示更新

### Requirement: 侧边栏步骤必须可点击跳转
系统 SHALL 允许用户点击侧边栏中的任意步骤快速跳转到对应页面。

#### Scenario: 用户点击侧边栏步骤
- **WHEN** 用户点击"视频"步骤
- **THEN** 页面跳转到 `/script-projects/{id}/video`

### Requirement: 当前步骤必须显示引导提示
系统 SHALL 在当前步骤处显示引导提示，帮助用户了解该步骤的操作要点。

#### Scenario: 用户在视频页面
- **WHEN** 用户当前在视频生成步骤
- **THEN** 侧边栏显示"生成视频分段的引导提示"
- **AND** 提示文字位于当前步骤下方

### Requirement: 流程推进必须自动点亮下一步
系统 SHALL 根据用户操作进度自动更新流程状态，前一步完成后点亮下一步。

#### Scenario: 用户完成资产生成
- **WHEN** 用户在资产页面完成资产生成
- **THEN** 资产步骤标记为完成
- **AND** 视频步骤自动变为进行中状态

## MODIFIED Requirements
### Requirement: 项目工作区导航布局从横向 Tab 改为左侧侧边栏
系统 SHALL 将 `ScriptProjectWorkflowNav` 组件从横向 Tab 布局改为垂直侧边栏布局，以支持流程可视化指引。

#### Scenario: 访问项目页面
- **WHEN** 用户访问任何项目相关页面
- **THEN** 左侧侧边栏显示流程步骤
- **AND** 当前步骤高亮显示
- **AND** 步骤按顺序编号展示

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为布局升级和功能增强，不移除现有功能。
**Migration**: 无需迁移。