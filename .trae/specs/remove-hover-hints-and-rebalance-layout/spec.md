# 后台悬停提示移除与布局比例重整 Spec

## Why
当前后台中存在较多鼠标悬停后出现的英文说明与小提示，视觉噪声较高。用户希望移除该类提示，并同步优化下拉框与导航窗口的尺寸比例，提升整体排版一致性与易读性。

## What Changes
- 全面移除后台页面中由 hover 触发的英文 tooltip 与弱提示文案显示
- 保留必要的中文显性信息，不依赖 hover 才能获取关键操作说明
- 统一调整下拉框的高度、内边距、宽度策略与对齐方式，优化在不同页面的视觉一致性
- 统一调整导航窗口（侧边导航/顶部导航相关容器）的宽度、间距与主内容区比例
- 优化关键页面排版节奏（标题、筛选区、导航区、内容区）以减少拥挤与错位

## Impact
- Affected specs: 后台壳层导航、表单与筛选交互、页面信息呈现规范
- Affected code: `aigc-site-react/src/layouts`、`aigc-site-react/src/components/layout`、`aigc-site-react/src/components/common`、`aigc-site-react/src/components/**` 中含 tooltip/select 的组件、`aigc-site-react/src/styles`

## ADDED Requirements
### Requirement: 无
本次不新增独立业务能力，仅进行交互与视觉规范优化。

## MODIFIED Requirements
### Requirement: 后台悬停提示与布局比例规范
系统 SHALL 在后台管理端移除非必要 hover 小提示（尤其英文 tooltip），并通过可见文案与统一布局规范提供清晰信息层级。

#### Scenario: 移除悬停英文提示
- **WHEN** 用户在后台页面将鼠标悬停在按钮、标签、图标、字段标题等元素上
- **THEN** 页面不再弹出英文 tooltip 或仅用于装饰的弱提示信息

#### Scenario: 下拉框比例统一
- **WHEN** 用户在任一列表页或配置页查看筛选区与表单区下拉框
- **THEN** 下拉框在高度、内边距、宽度与文本对齐上保持一致，不出现明显尺寸跳变

#### Scenario: 导航与内容区排版优化
- **WHEN** 用户在后台壳层切换页面并查看导航与主内容区域
- **THEN** 侧边导航、顶部导航与主内容区比例更平衡，内容区排版不拥挤且留白一致

## REMOVED Requirements
### Requirement: 依赖 hover 暴露补充说明
**Reason**: 悬停触发的英文或弱提示增加视觉干扰，且移动端不可用，影响理解一致性。  
**Migration**: 将必要说明迁移为可见中文文案、字段说明或分组标题，不再依赖 hover 才能获取关键信息。
