# 无限画布 AIGC 工具正式化 Spec

## Why
当前仓库已经存在 `/canvas` 占位路由、LiteGraph 画布组件、画布远端保存接口和 Comfy 代理，但仍停留在实验演示态，尚未成为本工程中可稳定访问、可保存、可提交、可回显的正式前端工具。

用户希望参考 `/Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/ComfyUI-0.19.1` 中的前端交互与节点式编排思路，在本工程内复制一套“无限画布类 AIGC 工具”体验；因此需要明确“复用哪些能力、按本工程怎样落地、MVP 到哪一步可交付”。

## What Changes
- 将现有 `/canvas` 从“开发中占位页”升级为本工程正式可用的无限画布工具页。
- 复用本仓库已接入的 `@comfyorg/litegraph`、本地/远端画布存储、`/api/comfy` 代理与 `CanvasGraph` 后端接口，不整套照搬 `ComfyUI-0.19.1` 应用壳。
- 参考 `ComfyUI-0.19.1` 的节点编辑、拖拽缩放、JSON 导入导出、节点目录与执行提交流程，抽取其中适合当前平台的前端交互模式。
- 对标本工程既有工具页模式，补齐导航入口、页面头部、帮助说明、权限边界、项目绑定、状态提示与结果预览。
- 定义第一阶段 MVP：先支持单画布编辑、基础节点操作、草稿保存恢复、Comfy prompt 提交、执行状态提示与结果回显。
- 定义第二阶段扩展位：节点模板库、多画布管理、项目级共享、媒体资源回写、工作流预设、权限协作。
- **BREAKING**：`/canvas` 不再只是“敬请期待”占位页，导航中的“开发中”语义需移除并改为真实工具入口。

## Impact
- Affected specs: 创作工具导航、工具页访问策略、AIGC 任务提交流程、画布草稿存储、项目资产关联
- Affected code: `aigc-site-react/src/pages/CanvasPage.tsx`、`aigc-site-react/src/components/canvas/ComfyLikeCanvas.tsx`、`aigc-site-react/src/components/layout/TopNav.tsx`、`aigc-site-react/src/components/layout/AppShellNav.tsx`、`aigc-site-react/src/router.tsx`、`aigc-site-react/src/lib/graph/*`、`aigc-site-react/src/stores/canvasGraphStore.ts`、`aigc-server/src/main/java/com/example/aigc/controller/CanvasGraphController.java`、`aigc-server/src/main/java/com/example/aigc/service/CanvasGraphService.java`、`aigc-server/src/main/java/com/example/aigc/controller/ComfyProxyController.java`

## ADDED Requirements
### Requirement: 系统必须提供正式可用的无限画布工具页
系统 SHALL 在本工程内提供一个正式可访问的无限画布 AIGC 工具页，而不是仅保留静态占位说明。

#### Scenario: 用户进入无限画布工具
- **WHEN** 用户从首页、侧边导航或工具入口进入 `/canvas`
- **THEN** 页面展示真实画布、工具栏、状态区与基础帮助信息
- **AND** 用户可以直接进行节点编辑，而不是只看到“开发中”文案

### Requirement: 无限画布工具必须复用 ComfyUI 交互范式但适配本工程壳层
系统 SHALL 参考 `ComfyUI-0.19.1` 的节点式画布交互，复用适合当前平台的拖拽、缩放、节点连接、JSON 导入导出与执行提交流程，同时保持本工程的布局、鉴权、导航与视觉体系一致。

#### Scenario: 用户在画布中进行节点式编排
- **WHEN** 用户在画布中添加、移动、连接、删除节点
- **THEN** 交互行为与 ComfyUI 类似，具备无限平移、缩放和节点连线体验
- **AND** 页面视觉、按钮、提示与导航仍符合本工程现有工具页风格

### Requirement: 系统必须支持画布草稿保存与恢复
系统 SHALL 支持无限画布草稿的本地缓存与远端保存恢复，并允许后续扩展为多草稿管理。

#### Scenario: 用户刷新后恢复画布
- **WHEN** 用户修改节点图后离开页面或刷新页面
- **THEN** 系统自动保存当前画布草稿
- **AND** 用户再次进入时可恢复最近一次草稿内容与视口位置

#### Scenario: 用户选择绑定工程
- **WHEN** 用户在无限画布工具中选择关联某个剧本工程或项目
- **THEN** 草稿记录可带上项目关联信息
- **AND** 未绑定工程时仍允许用户使用画布核心功能

### Requirement: 系统必须支持基于 Comfy 代理提交工作流
系统 SHALL 将前端画布图转换为可提交的 prompt 结构，并通过现有 `/api/comfy` 代理发起执行请求。

#### Scenario: 用户提交画布到 Comfy
- **WHEN** 用户在工具栏点击执行或提交
- **THEN** 前端将当前图结构转换为符合后端代理要求的请求体
- **AND** 请求经由本工程现有代理接口转发到 Comfy 服务
- **AND** 页面展示提交成功、排队中、完成或失败状态

### Requirement: 系统必须展示最小可用的执行结果回显
系统 SHALL 在第一阶段提供最小可用的执行结果回显能力，至少让用户知道任务是否完成，并能查看结果引用或失败原因。

#### Scenario: 任务执行完成
- **WHEN** 用户提交的 Comfy 工作流执行完成
- **THEN** 页面显示任务完成状态
- **AND** 若返回图片或结果对象，则页面展示可预览的结果区域或结果摘要

#### Scenario: 任务执行失败
- **WHEN** 提交、轮询或结果解析失败
- **THEN** 页面展示明确错误信息
- **AND** 不应静默失败

### Requirement: 系统必须提供受控的节点来源策略
系统 SHALL 明确区分“第一阶段内置节点”和“后续动态同步节点”，避免一开始直接暴露完整 ComfyUI 节点生态导致不可控。

#### Scenario: 第一阶段画布初始化
- **WHEN** 用户首次打开无限画布工具
- **THEN** 系统先提供一组经过筛选的内置节点或模板
- **AND** 动态同步 Comfy 节点能力作为增强项按需开启

### Requirement: 系统必须与本工程工具入口和角色策略对齐
系统 SHALL 让无限画布工具在导航、首页快捷入口、页面标题和角色边界上与现有工具体系保持一致。

#### Scenario: 已登录用户查看工具导航
- **WHEN** 学生、老师或管理员登录后进入工具导航
- **THEN** 可以看到无限画布入口
- **AND** 入口文案不再标记为“开发中”

## MODIFIED Requirements
### Requirement: `/canvas` 页面必须从占位页升级为正式工具页
系统 SHALL 将当前 `/canvas` 路由从“敬请期待”的占位状态升级为正式的无限画布工具页，提供真实的编辑、保存和提交能力。

#### Scenario: 用户访问既有画布路由
- **WHEN** 用户访问 `/canvas`
- **THEN** 不再只显示静态说明与返回首页按钮
- **AND** 页面承接正式工具链路

### Requirement: 画布草稿保存策略必须从“单最新记录”演进为“可命名、可绑定、可扩展”
系统 SHALL 在兼容当前“保存最近一份草稿”行为的基础上，为后续多草稿、命名保存、项目绑定与共享预留数据结构与接口扩展位。

#### Scenario: 用户保存当前画布
- **WHEN** 用户主动保存或触发自动保存
- **THEN** 系统至少保存当前草稿内容、标题、更新时间和可选项目关联
- **AND** 现有接口可平滑扩展到多条记录管理

## REMOVED Requirements
### Requirement: `/canvas` 仅作为开发中占位入口
**Reason**: 用户明确希望将无限画布做成正式 AIGC 工具，继续保留占位态不符合交付目标。
**Migration**: 现有占位入口直接升级为正式工具页；保留原路由地址，避免前端导航和历史书签失效。
