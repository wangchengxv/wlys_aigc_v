# 实现细节索引（Implementation Detail Index）

> 目标：为 AI Agent 提供“先定位、再改动”的实现地图，覆盖状态管理、组件复用、请求副作用、高风险区与最小验证路径。  
> 范围：`aigc-site-react`（前端主流程）+ 与其强耦合的后端 API 入口。

## 1. 状态管理索引

### 1.1 核心业务状态（Zustand）

| 领域 | 真实位置 | 关键职责 | 常见改动风险 |
| --- | --- | --- | --- |
| 剧本工程全链路状态 | `aigc-site-react/src/stores/scriptProjectStore.ts` | 统一维护项目详情、剧本、资产、镜头、视频/配音/口型/剪辑/导出任务、审核状态与轮询逻辑 | 并发请求与轮询状态交叉，容易出现旧项目状态覆盖当前项目（`activeProjectId`、`shouldApplyProjectState`） |
| 认证会话状态 | `aigc-site-react/src/stores/authStore.ts` | 登录态初始化、刷新、登录、退出 | `init()` 与 `AppLayout` 首屏判定强耦合，误改会导致未登录/白屏/循环跳转 |
| 全局创作设定 | `aigc-site-react/src/stores/globalSettingsStore.ts` | 画幅、风格模式、目标时长等跨页面共享参数，持久化到 `localStorage` | 字段校验改动会影响流程门禁（全局设定是否完成） |
| 工作台生成状态 | `aigc-site-react/src/stores/generationStore.ts` | 生成任务列表、收藏、当前任务 | 异步请求序号 `generateRequestSeq` 控制竞态，误删会导致“后返回覆盖先返回” |
| 风格模板状态 | `aigc-site-react/src/stores/styleTemplateStore.ts` | 远端模板加载与本地回退 | `loadToken` 竞态保护被破坏后会出现旧请求覆盖新请求 |
| 画布状态（本地 + 远端） | `aigc-site-react/src/stores/canvasGraphStore.ts` | 图状态本地缓存、远端自动保存、远端同步 | `setState` 带副作用写远端；改动需避免频繁触发保存风暴 |
| 主题状态 | `aigc-site-react/src/stores/themeStore.ts` | 主题初始化与切换（当前仅 `light`） | 影响入口渲染属性 `document.documentElement.dataset.theme` |

### 1.2 状态初始化入口

- `aigc-site-react/src/main.tsx`：应用启动时执行 `useThemeStore.getState().initTheme()`、`useGlobalSettingsStore.getState().init()`。
- `aigc-site-react/src/layouts/AppLayout.tsx`：页面壳层初始化时触发 `useAuthStore().init()`、`useStyleTemplateStore().loadTemplates()`。

## 2. 组件复用索引

### 2.1 壳层与流程导航复用

| 组件/模块 | 真实位置 | 复用范围 | 说明 |
| --- | --- | --- | --- |
| 项目子页面壳 | `aigc-site-react/src/components/script/ProjectSubpageShell.tsx` | `script-projects/:projectId/*` 多页面 | 提供统一 hero、stats、toolbar、步骤切换、全局设定门禁提示 |
| 项目流程侧栏 | `aigc-site-react/src/components/script/ScriptProjectWorkflowNav.tsx` | 项目流页面统一导航 | 基于 `scriptProjectWorkflowSteps` 计算当前步骤、锁定后续步骤 |
| 流程步骤定义 | `aigc-site-react/src/components/script/scriptProjectWorkflow.ts` | 导航、步骤切换、当前步骤判定 | 单一真相源，改动将同步影响多个页面 |
| 全站应用壳 | `aigc-site-react/src/layouts/AppLayout.tsx` | 路由根布局 | 控制登录前后视图、通用导航与页头 |

### 2.2 交付链路页面复用锚点

- 视频剪辑页：`aigc-site-react/src/pages/ScriptProjectFinalCompositionPage.tsx`（复用 `ProjectSubpageShell` + `PipelineProgressBar`）。
- 导出交付页：`aigc-site-react/src/pages/ScriptProjectExportPage.tsx`（复用 `ProjectSubpageShell` + 交付来源选择逻辑）。
- 视频生产页：`aigc-site-react/src/pages/ScriptProjectVideoPage.tsx`（复用 `VideoSegmentCard`、`WorkflowModelPanel`）。

## 3. 请求与副作用索引

### 3.1 请求层（API Client）

| 模块 | 真实位置 | 副作用/约束 |
| --- | --- | --- |
| 统一 API 客户端 | `aigc-site-react/src/api/index.ts` | Axios 请求/响应拦截、token 注入、错误增强、Mock 分支、本地存储读写、URL 解析 |
| 剧本 API 运行前置检查 | `aigc-site-react/src/api/index.ts` 中 `requireScriptApi()` | 无 `VITE_API_BASE_URL` 时直接抛错，阻断剧本链路 |
| 视频剪辑协议归一化 | `aigc-site-react/src/lib/scriptProject/videoEditingContract.ts` | 前后端字段兼容、毫秒/秒转换、状态归一化；影响剪辑任务与草稿一致性 |
| 交付来源选择守卫 | `aigc-site-react/src/lib/scriptProject/videoEditingPageGuards.ts` | 发布条件判定与“发布 > 预览 > 回退”来源选择 |

### 3.2 页面/Store 侧副作用入口

- `aigc-site-react/src/stores/scriptProjectStore.ts`：
  - `hydrate()` 并发拉取多类资源。
  - `startPolling()/stopPolling()` 定时轮询并按 `projectStatus` 停止。
  - 视频剪辑草稿在远端失败时回退本地持久化（`localStorage`）。
- `aigc-site-react/src/pages/ScriptProjectFinalCompositionPage.tsx`：
  - `useEffect` 首次加载后按状态启动轮询。
  - 草稿编辑、保存、预览、发布串联多个异步动作。
- `aigc-site-react/src/pages/ScriptProjectExportPage.tsx`：
  - 初始化并发拉取交付链路所需数据。
  - 根据流水线状态决定是否持续轮询。

### 3.3 后端强耦合入口（请求落点）

- `aigc-server/src/main/java/com/example/aigc/controller/ScriptProjectController.java`
- `aigc-server/src/main/java/com/example/aigc/controller/VideoPipelineController.java`
- `aigc-server/src/main/java/com/example/aigc/controller/StoryboardLiteController.java`

> 说明：前端 `src/api/index.ts` 中 `/api/v1/script-projects/*`、`/video-editing/*`、`/storyboard-lite/*`、`/content-review/*` 请求与以上控制器强耦合。

## 4. 高风险改动区

| 风险级别 | 区域 | 真实位置 | 高风险原因 |
| --- | --- | --- | --- |
| 高 | 剧本工程总 Store | `aigc-site-react/src/stores/scriptProjectStore.ts` | 状态字段多、流程长、异步并发与轮询交织，误改易引发级联回归 |
| 高 | API 聚合层 | `aigc-site-react/src/api/index.ts` | 入口函数多且共享拦截器/错误处理/token/Mock，局部改动影响全局请求行为 |
| 高 | 剪辑协议转换层 | `aigc-site-react/src/lib/scriptProject/videoEditingContract.ts` | 字段兼容与单位转换出错会直接破坏剪辑草稿、渲染任务、发布逻辑 |
| 中高 | 剪辑工作台页 | `aigc-site-react/src/pages/ScriptProjectFinalCompositionPage.tsx` | 本地表单态与 store 态双写，保存/渲染/发布链路紧耦合 |
| 中高 | 导出交付页 | `aigc-site-react/src/pages/ScriptProjectExportPage.tsx` | 交付来源选择、审核状态、下载入口聚合，逻辑分支多 |
| 中 | 流程导航与门禁 | `aigc-site-react/src/components/script/ProjectSubpageShell.tsx`、`aigc-site-react/src/components/script/ScriptProjectWorkflowNav.tsx` | 全局设定门禁规则跨页面生效，误改会导致步骤可达性异常 |
| 中 | 路由注册 | `aigc-site-react/src/router.tsx` | 路由路径与页面懒加载绑定，改动不当会造成入口丢失 |

## 5. 最小验证路径（MVP）

### 5.1 静态与契约校验（先跑）

在仓库根目录执行：

```bash
cd aigc-site-react
npm run lint
npm run test:video-editing-contract
npm run test:video-editing-pages
```

### 5.2 关键手测路径（至少覆盖一遍）

1. 项目流程门禁：进入 `script-projects/:projectId/preview`，未完成全局设定时应出现门禁提示并可跳转到 `global-settings`。
2. 视频链路：在 `script-projects/:projectId/video` 执行“拆分镜头 -> 启动视频生成”，确认任务状态可轮询更新。
3. 剪辑链路：在 `script-projects/:projectId/final-composition` 执行“保存草稿 -> 生成预览 -> 发布成片”，确认发布仅在预览版本匹配时可进行。
4. 导出链路：在 `script-projects/:projectId/export` 检查成片来源优先级是否为“已发布剪辑 > 预览 > 自动成片回退”，并验证下载按钮行为。
5. 审核链路：同页执行“提交审核（或重新提审）-> 审核通过/驳回”，确认状态、意见与记录刷新。

### 5.3 回归关注点（高优先）

- 请求失败回退：后端不可用时，剪辑草稿是否仍可本地保存并恢复。
- 轮询停止条件：任务完成后是否自动停止，离开页面是否触发 `stopPolling()`。
- 竞态一致性：快速切换项目时，不应把 A 项目数据写入 B 项目页面。
