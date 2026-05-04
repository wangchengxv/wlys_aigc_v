# 剧本闭环Lite 模型选择区霓虹UI升级计划

## Summary
- 目标：在 `剧本闭环Lite` 页面中，仅升级“模型选择区”UI/UX（不改业务流程），重点覆盖 `Step2Script`（图片模型）与 `Step4Video`（视频模型）。
- 风格：采用“科技霓虹感”，并保持轻动效优先，避免重动画造成干扰或性能风险。
- 技术策略：抽象可复用模型选择组件，统一两步中的模型输入方式、模型下拉、状态提示与辅助信息展示。
- 选择方式：保留原生 `select` 交互（增强容器与状态样式），保证可访问性与兼容性。
- 信息展示：模型项展示“显示名 + provider（并带模型ID）”，提升可辨识度与选择效率。

## Current State Analysis
- 路由入口已存在：`/tools/storyboard-lite` 对应 `aigc-site-react/src/pages/storyboard-lite/index.tsx`。
- 第二步模型区位于 `aigc-site-react/src/pages/storyboard-lite/components/Step2Script.tsx`：
  - 当前是“输入方式 select + 当前模型 select/输入框 + 刷新按钮 + 文案”的基础表单布局。
  - 视觉层级较弱，缺少卡片化分组、选中态强化、焦点态引导和模型信息摘要。
- 第四步模型区位于 `aigc-site-react/src/pages/storyboard-lite/components/Step4Video.tsx`：
  - 结构与第二步类似，且存在与第二步重复的模型选择逻辑与展示模式。
  - 目前缺少统一化视觉规范，用户在跨步骤操作时感知割裂。
- 页面样式文件为 `aigc-site-react/src/pages/storyboard-lite/StoryboardLite.css`：
  - 已有卡片、网格、按钮与步骤条基础样式。
  - 尚无“模型选择专区”的专属样式模块（例如霓虹边框、光晕、状态标签、统一容器）。
- 通用输入控件能力：
  - `AppInput` 已可承载手动输入模型ID。
  - 全局样式中对 `select` 有统一皮肤，可在 Lite 局部通过更高优先级类名做增强，而不影响其他页面。

## Proposed Changes

### 1) 新增可复用模型选择组件（核心）
- 文件：`aigc-site-react/src/pages/storyboard-lite/components/ModelSelectorPanel.tsx`（新增）
- 做什么：
  - 抽象 Step2/Step4 共用 UI：标题、说明、副文案、输入方式切换、预设模型下拉/手动输入、刷新入口、状态提示。
  - 通过 props 传入差异化文案与数据源（图片模型/视频模型），复用同一交互框架。
  - 在预设模式下继续使用原生 `select`，但外层使用霓虹风容器（带强调边框、光晕、聚焦态）。
- 为什么：
  - 消除重复代码，统一交互心智，后续可扩展到其他工作流页面。
  - 降低未来样式迭代成本（只维护一个组件）。
- 如何做：
  - 设计通用 props：`mode`、`setMode`、`selectedModel`、`setSelectedModel`、`customModel`、`setCustomModel`、`options`、`details`、`loading`、`onRefresh`、`variant(image|video)`、文案配置。
  - 内部复用已有 `labelForModel` 逻辑并增强文案：展示 `displayName / provider / modelName` 组合。
  - 为模式切换按钮增加 `aria-pressed` 与焦点样式，确保键盘可用性。

### 2) 接入第二步与第四步页面
- 文件：`aigc-site-react/src/pages/storyboard-lite/components/Step2Script.tsx`
- 做什么：
  - 移除本地模型区重复结构，替换为 `ModelSelectorPanel`。
  - 保留“刷新模型列表”和“本机记忆”提示，但并入统一面板底部信息区。
- 为什么：
  - 保持第二步业务逻辑不变，仅提升展示与可用性。
- 如何做：
  - 透传现有 `imageModel*` 状态和 `handleRefreshModels`。
  - 继续保留剧本输入与三视图提示词区域，不扩大改造范围。

- 文件：`aigc-site-react/src/pages/storyboard-lite/components/Step4Video.tsx`
- 做什么：
  - 同步替换视频模型配置区为 `ModelSelectorPanel`。
  - 保持“首帧来源”与“视频提示词”逻辑不变，仅重排模型区域视觉层级。
- 为什么：
  - 跨步骤一致体验，降低学习成本。
- 如何做：
  - 透传现有 `videoModel*` 状态，使用 `variant='video'` 或对应文案配置。

### 3) 增加 Lite 页面的模型区霓虹样式层
- 文件：`aigc-site-react/src/pages/storyboard-lite/StoryboardLite.css`
- 做什么：
  - 新增模型选择区专属样式命名空间（例如 `sl-model-panel*`）。
  - 实现科技霓虹感：渐变描边、柔光阴影、hover/focus 光晕、轻微位移动效。
  - 增加状态表达：加载中、不可用、当前模式高亮、选中模型摘要区。
- 为什么：
  - 在不更改全局设计系统的前提下，实现局部高识别 UI 升级。
- 如何做：
  - 仅在 `storyboard-lite` 局部类下生效，避免污染全局 `select` 与 `input`。
  - 使用轻量 CSS 过渡（`transition`）替代复杂关键帧动画。
  - 补充响应式规则，确保窄屏下模型面板不会挤压错位。

### 4) 类型与可维护性清理（如需要）
- 文件：`aigc-site-react/src/pages/storyboard-lite/components/Step2Script.tsx`
- 文件：`aigc-site-react/src/pages/storyboard-lite/components/Step4Video.tsx`
- 做什么：
  - 合并重复的 `ModelInputMode` 声明，避免多处重复定义。
  - 将模型展示辅助函数统一放入新组件或小工具函数，减少重复。
- 为什么：
  - 防止后续修改时出现两个步骤展示不一致。
- 如何做：
  - 以“最少移动、最小影响”为原则，不触碰与本次需求无关的业务逻辑。

## Assumptions & Decisions
- 已确认决策：
  - 视觉方向：科技霓虹感。
  - 范围：仅模型选择区（不做整页大改版）。
  - 交互：轻动效优先。
  - 架构：抽可复用组件。
  - 预设选择：保留 `select` 增强样式。
  - 信息密度：展示显示名 + provider（并保留模型ID可识别性）。
- 范围边界：
  - 不调整会话创建、关键帧确认、视频生成接口调用与状态机逻辑。
  - 不改动后端 API 与数据结构。

## Verification Steps
- 代码级检查：
  - TypeScript 编译通过，新增组件类型无报错。
  - Step2/Step4 不再重复维护同构模型选择 DOM。
- 交互验收：
  - 可在两步中正常切换“预设/手动输入”模式。
  - 预设模式下可选模型正常渲染，显示名/provider 显示正确。
  - 手动输入模式下模型ID可编辑且与原流程一致。
  - 刷新模型列表后，面板状态与可选项正确更新。
- 视觉验收：
  - 模型面板具备霓虹风视觉识别（描边、光晕、选中态、焦点态）。
  - 动效轻量，不影响输入与下拉操作。
  - 桌面与窄屏布局均无明显错位。
- 回归关注点：
  - 本地模型偏好记忆（localStorage）继续生效。
  - 生成三视图与生成视频流程行为与升级前一致。
