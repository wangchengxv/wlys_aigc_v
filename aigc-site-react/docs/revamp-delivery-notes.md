# Revamp Delivery Notes

## Meta
- Project: `aigc-site-react`
- Release: 前端改版收尾 Task1/2/5 封板交付记录
- Owner: 前端改版执行人
- Delivery Date: 2026-04-17
- Environment: macOS / Node 工程本地构建环境

## Delivered Items
- 固定路由与工作台多入口兼容核查
  - `src/router.tsx` 已确认保留：
    - `/workspace`
    - `/tools/image`
    - `/tools/video`
    - `/tools/image-to-video`
    - `/courses`、`/courses/:courseId`
    - `/script-projects`、`/script-projects/:projectId`
    - `/admin/media-resources`
    - `/settings`
  - 工作台模式由 route handle 元数据 `workspaceMode/workspaceVariant` 显式驱动
- 截图资产归档与索引完成（Task1/2）
  - 截图目录：`aigc-site-react/docs/revamp-screenshots/2026-04-17`
  - 归档数量：`32` 张（`8` 页面 `x 4` 视口）
  - 命名规则：`{page}-w{viewport}.png`，视口为 `1440/1024/768/390`
  - 页面范围：`home`、`workspace`、`courses`、`courses-1`、`script-projects`、`script-projects-1`、`admin-media-resources`、`settings`
  - 索引字段：页面、视口、角色、文件路径、结果（已回填到 `revamp-acceptance-checklist.md`）
- 角色边界与管理员页直访拦截核查
  - 导航收口：`src/components/layout/AppShellNav.tsx` 基于 `roles` 过滤菜单
  - 页面守卫：
    - `MediaResourcesPage`、`ModelConfigPage`、`ProviderHubPage`、`AdminDirectoryPage`、`AuditLogsPage` 均对非管理员显示“仅管理员可访问”
  - 结论：导航隐藏 + 页面内守卫双层生效
- 响应式核查记录补齐（四档视口）
  - 样式断点覆盖：
    - `global.css`: `1180/960/640`
    - `components.css`: `1100/960/900/800/768/480`
    - `index.css`: `1024`
  - 关键区域：导航抽屉、顶部工具条、筛选条、固定 dock、弹窗/抽屉
- HelpHint 统一收口
  - `ProjectSubpageShell` 已内聚 `HelpHint` 渲染，统一承接 `helpTitle/help` 语义位
  - 项目链路次级页（预览、资产、配音、口型、成片、导出等）传入的帮助信息已统一落到同一帮助入口形态
- 创建交互抽屉化收口
  - `ScriptProjectCreatePage` 首屏改为说明区 + 操作入口，创建表单迁移到 `ActionDrawer`
  - 首屏不再由创建大表单占位，保留“打开创建表单”显式动作
- 技术基线执行结果回填
  - `npm run lint`：通过（`eslint .`，exit code 0）
  - `npm run build`：通过（`tsc -b && vite build`，exit code 0）
  - 构建结果显示已分包（`react-vendor`、`router-vendor`、`network-vendor`、路由级 chunk）
  - 核心产物（本次实跑）：
    - `react-vendor` 约 `189.64 kB`
    - `router-vendor` 约 `92.89 kB`
    - `network-vendor` 约 `36.30 kB`
    - `index` 约 `29.09 kB`

## Known Issues
- 本轮封板范围内无阻断交付的已知问题

## Risks And Follow-ups
- Risk: 新增页面若绕开 `ProjectSubpageShell`，可能再次出现帮助入口不一致
- Follow-up:
  - 约束项目链路页优先复用 `ProjectSubpageShell` 的 `helpTitle/help` 能力
  - 对非项目链路页在文档中声明统一替代形态并保持单轨
- Risk: 后续页面新增后可能出现截图索引遗漏
- Follow-up:
  - 将“关键页面四档截图 + 索引回填”纳入每轮 UI 封板准入条件

## Verification
- Build/Deploy Check:
  - `npm run lint` 通过，无 warning/error 输出
  - `npm run build` 通过，vite 产物已完成分包，未出现阻断性构建告警（built in `130ms`）
  - 主要产物（节选）：
    - `react-vendor` 约 `189.64 kB`
    - `router-vendor` 约 `92.89 kB`
    - `network-vendor` 约 `36.30 kB`
    - `index` 约 `29.09 kB`
    - `ScriptProjectExportPage` 约 `28.47 kB`
- Smoke Test Result:
  - 本轮基于 `2026-04-17` 批次 `32` 张截图完成了关键页面四档核查，结论满足 Task1/2/5 封板要求
  - 文档、截图索引、构建基线与 checklist 状态已一致

## References
- PR/Commit: 待补充
- Related Docs:
  - `aigc-site-react/docs/revamp-acceptance-checklist.md`
  - `.trae/specs/complete-revamp-tail-acceptance/tasks.md`
  - `.trae/specs/complete-revamp-tail-acceptance/checklist.md`
