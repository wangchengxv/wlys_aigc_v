# 剧本闭环Lite UI重构与排版优化计划

## 1. 摘要 (Summary)
将当前垂直平铺的 `StoryboardLitePage` 重构为**“向导式流畅流程（Wizard Flow）”**。通过拆分长文件为多个子组件、引入专属的 CSS 动效与现代化布局，提升工具的专业感、沉浸感与代码可维护性。

## 2. 当前状态分析 (Current State Analysis)
- **文件结构**：所有 UI 和状态逻辑（约 500 行）集中在单一文件 `src/pages/StoryboardLitePage.tsx` 中。
- **页面布局**：4 个核心步骤（创建会话、剧本与模型、关键帧确认、生成视频）以 `.panel.glass` 卡片形式自上而下垂直堆叠。
- **视觉体验**：信息密度较高，缺乏流程引导，用户在长页面中滚动容易失去焦点，整体风格偏向基础后台表单，缺乏独立创作工具的精致感。

## 3. 拟议变更 (Proposed Changes)

### 3.1 目录重构与组件拆分
新建 `src/pages/storyboard-lite/` 目录，将原单文件拆分：
- **`index.tsx`**：作为页面入口与状态容器，集中管理 `session`, `activeStep`, `models` 等核心状态，并导出 `StoryboardLitePage`。
- **`StoryboardLite.css`**：新增专属样式文件，包含向导布局、步骤切换动效（如 `slideUpFade`）、精致的卡片阴影与表单排版。
- **`components/StepIndicator.tsx`**：顶部横向步骤指示器，显示“1. 创建会话 -> 2. 剧本与三视图 -> 3. 关键帧确认 -> 4. 视频生成”，支持点击已完成的步骤进行回退。
- **`components/Step1Session.tsx`**：渲染第一步的表单（项目ID、标题）与“下一步”按钮。
- **`components/Step2Script.tsx`**：渲染第二步（模型选择、剧本输入、三视图提示词）与生成逻辑。
- **`components/Step3Keyframe.tsx`**：渲染第三步的大画幅网格预览与关键帧确认操作。
- **`components/Step4Video.tsx`**：渲染第四步的视频生成配置（首帧来源、视频模型、提示词）与最终视频结果展示。

### 3.2 路由更新
- **修改 `src/router.tsx`**：将 `tools/storyboard-lite` 路由的组件引入路径从 `@/pages/StoryboardLitePage` 修改为 `@/pages/storyboard-lite`。
- **清理文件**：删除原 `src/pages/StoryboardLitePage.tsx`。

### 3.3 交互与 UI 设计细节 (What/Why/How)
- **What**：引入 `activeStep` (1~4) 状态，单屏仅展示当前激活的步骤内容。
- **Why**：降低用户的认知负担，打造“向导式流畅流程”，符合轻量级闭环工具的定位。
- **How**：在 `StoryboardLite.css` 中添加 `.sl-step-container` 和 `@keyframes fadeSlideUp`，当 `activeStep` 切换时，旧组件卸载，新组件挂载并触发进场动画。为输入框和按钮增加更高的内边距（padding）与圆角（border-radius），提升“编辑风”质感。

## 4. 假设与决策 (Assumptions & Decisions)
- **状态管理**：由于只是单页面内的多步骤表单，假设不引入 Redux/Zustand 新 store，继续在 `index.tsx` 中使用 React 原生 `useState` / `useMemo`，通过 Props 传递给各 Step 子组件。
- **样式方案**：依照用户“仅在现有框架内重构、允许引入新样式/动效”的决定，不引入大型第三方动画库（如 framer-motion），仅通过原生 CSS 动画实现流畅的视觉过渡。
- **模型刷新逻辑**：复用原有的 `loadLiteModelPrefs` 和获取模型的 API 逻辑，仅在 UI 层进行封装分离。

## 5. 验证步骤 (Verification Steps)
1. 启动本地开发服务器，访问 `/tools/storyboard-lite`。
2. 验证顶部是否出现 4 步进度条，且初始状态下只显示“第一步：创建会话”。
3. 验证在创建会话后，点击“下一步”能否平滑过渡到剧本页面，且无报错。
4. 验证核心数据流（填剧本 -> 生成关键帧 -> 确认关键帧 -> 生成视频）在各子组件间传递是否正常，接口调用是否成功。
5. 验证旧的路由加载和 `aigc_storyboard_lite_model_prefs_v1` 本地缓存逻辑是否依然生效。
