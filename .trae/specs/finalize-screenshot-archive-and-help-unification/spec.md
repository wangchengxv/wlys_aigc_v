# 截图归档与帮助入口统一收口 Spec

## Why
当前改版文档已补齐文字验收，但仍缺少可追溯截图资产；同时 HelpHint 与抽屉/弹窗形态尚未完成全量统一，需要最终收口以达成交付闭环。

## What Changes
- 补齐 1440/1024/768/390 四档关键页面截图并归档到 docs
- 在验收文档中绑定截图路径、页面、视口、角色与通过状态
- 对项目链路页面统一帮助入口形态：优先使用 HelpHint，若不适用则统一替代规则
- 对创建/编辑动作进行逐页核查并统一为抽屉或弹窗承载，消除首屏表单占位
- 更新交付说明与验收结论，明确本轮收口范围、遗留项与后续建议

## Impact
- Affected specs: `complete-revamp-tail-acceptance`, `simplify-progressive-disclosure-ui`, `remove-hover-hints-and-rebalance-layout`
- Affected code: `aigc-site-react/docs/`, `aigc-site-react/src/pages/`, `aigc-site-react/src/components/common/HelpHint.tsx`, `aigc-site-react/src/components/common/ActionDrawer.tsx`, `aigc-site-react/src/components/common/ConfirmDialog.tsx`

## ADDED Requirements
### Requirement: 验收截图资产归档
系统 SHALL 为固定验收页面提供可追溯截图资产，并在文档中记录其元信息。

#### Scenario: 四档截图可追溯
- **WHEN** 执行改版验收
- **THEN** 每个关键页面在 1440/1024/768/390 四档至少有对应截图记录和结果状态

## MODIFIED Requirements
### Requirement: 帮助入口与创建编辑交互统一性
系统 SHALL 在项目链路及后台核心页面中保持帮助说明和创建/编辑交互形态一致，避免同类场景体验割裂。

#### Scenario: HelpHint 统一
- **WHEN** 用户进入项目次级页或高信息密度页面
- **THEN** 页面帮助说明使用统一组件或统一替代规范，且文档中有明确说明

#### Scenario: 抽屉/弹窗统一
- **WHEN** 用户触发创建或编辑动作
- **THEN** 表单通过抽屉或弹窗承载，不再占据首屏主要内容区

## REMOVED Requirements
### Requirement: 仅文字验收而无截图资产
**Reason**: 文字结论难以复核 UI 细节与响应式表现。  
**Migration**: 将验收截图归档并在文档中建立页面-视口-角色映射，作为交付证据链。
