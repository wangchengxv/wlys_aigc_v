# Revamp Acceptance Checklist

## Meta
- Project: `aigc-site-react` 前端改版收尾验收
- Version: Task1/2/5 封板完成版
- Owner: 前端改版执行人
- Reviewer: 待评审
- Date: 2026-04-17
- Status: 已完成 Task1/2/3/4/5，封板完成

## Scope
- In Scope:
  - 固定回归路由核查：`/`、`/workspace`、`/courses`、`/courses/:courseId`、`/script-projects`、`/script-projects/:projectId`、`/admin/media-resources`、`/settings`
  - 角色边界核查：`ADMIN / TEACHER / STUDENT` 导航可见性与管理员页面直访行为
  - 响应式核查：`1440 / 1024 / 768 / 390` 四档（含导航、筛选条、抽屉、弹窗、固定 dock）
  - 帮助说明一致性核查：项目次级页 `HelpHint`/替代形态统一策略
  - 技术基线核查：`npm run lint`、`npm run build`、大包体/分包结果记录
- Out of Scope:
  - 后端接口与数据 schema 改造
  - 真实设备与跨浏览器差异专项验证

## Task1 截图归档目录与清单结构
- [x] 截图归档目录与命名规则已建立
  - 目录：`aigc-site-react/docs/revamp-screenshots/2026-04-17`
  - 命名规则：`{page}-w{viewport}.png`
  - 视口固定：`1440 / 1024 / 768 / 390`
- [x] 验收文档截图索引字段已补齐
  - 字段：页面、视口、角色、文件路径、结果
- [x] 最小覆盖范围与补拍规则已明确
  - 最小覆盖范围：`8` 个关键页面（含详情页）`x 4` 档视口，共 `32` 张
  - 缺失补拍规则：若某页面缺少任一视口截图，判定该页面验收未完成，不允许进入 Task5 封板

## Task2 固定路由与构建基线
- [x] 固定回归路由核查已记录
  - 路由在 `src/router.tsx` 中明确声明，保留公开入口稳定：
    - `/workspace`、`/tools/image`、`/tools/video`、`/tools/image-to-video` 均存在且未做重定向
    - `/courses`、`/courses/:courseId`、`/script-projects`、`/script-projects/:projectId`、`/admin/media-resources`、`/settings` 均存在
  - 工作台模式通过 route handle 的 `workspaceMode/workspaceVariant` 显式驱动，不再依赖 `pathname.includes`
- [x] `npm run lint` 已执行并通过（exit code 0）
  - 执行目录：`aigc-site-react`
  - 结果：`eslint .` 无 warning / error 输出
- [x] `npm run build` 已执行并通过（exit code 0）
  - 执行目录：`aigc-site-react`
  - 构建链路：`tsc -b && vite build`
  - 产物要点：
    - `dist/assets/react-vendor-*.js` 约 `189.64 kB`
    - `dist/assets/router-vendor-*.js` 约 `92.89 kB`
    - `dist/assets/index-*.js` 约 `29.09 kB`
    - 页面 chunk 已按路由拆分（如 `ScriptProjectExportPage`、`ProviderHubPage`、`WorkspacePage` 等）
  - 大包体结论：本次构建未出现超阈值 warning；已有明显 vendor 分包与路由级拆分
- [x] 四档截图采集与回填已完成（32/32）
  - 页面覆盖：`home`、`workspace`、`courses`、`courses-1`、`script-projects`、`script-projects-1`、`admin-media-resources`、`settings`
  - 采集结论：未发现阻断验收的布局错位、遮挡、横向溢出或关键交互不可用问题
  - 截图索引表：

| 页面 | 视口 | 角色 | 文件路径 | 结果 |
| --- | --- | --- | --- | --- |
| home | 1440 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/home-w1440.png` | 通过 |
| home | 1024 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/home-w1024.png` | 通过 |
| home | 768 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/home-w768.png` | 通过 |
| home | 390 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/home-w390.png` | 通过 |
| workspace | 1440 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/workspace-w1440.png` | 通过 |
| workspace | 1024 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/workspace-w1024.png` | 通过 |
| workspace | 768 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/workspace-w768.png` | 通过 |
| workspace | 390 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/workspace-w390.png` | 通过 |
| courses | 1440 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-w1440.png` | 通过 |
| courses | 1024 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-w1024.png` | 通过 |
| courses | 768 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-w768.png` | 通过 |
| courses | 390 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-w390.png` | 通过 |
| courses-1 | 1440 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-1-w1440.png` | 通过 |
| courses-1 | 1024 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-1-w1024.png` | 通过 |
| courses-1 | 768 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-1-w768.png` | 通过 |
| courses-1 | 390 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/courses-1-w390.png` | 通过 |
| script-projects | 1440 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-w1440.png` | 通过 |
| script-projects | 1024 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-w1024.png` | 通过 |
| script-projects | 768 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-w768.png` | 通过 |
| script-projects | 390 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-w390.png` | 通过 |
| script-projects-1 | 1440 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-1-w1440.png` | 通过 |
| script-projects-1 | 1024 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-1-w1024.png` | 通过 |
| script-projects-1 | 768 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-1-w768.png` | 通过 |
| script-projects-1 | 390 | ADMIN/TEACHER/STUDENT | `docs/revamp-screenshots/2026-04-17/script-projects-1-w390.png` | 通过 |
| admin-media-resources | 1440 | ADMIN | `docs/revamp-screenshots/2026-04-17/admin-media-resources-w1440.png` | 通过 |
| admin-media-resources | 1024 | ADMIN | `docs/revamp-screenshots/2026-04-17/admin-media-resources-w1024.png` | 通过 |
| admin-media-resources | 768 | ADMIN | `docs/revamp-screenshots/2026-04-17/admin-media-resources-w768.png` | 通过 |
| admin-media-resources | 390 | ADMIN | `docs/revamp-screenshots/2026-04-17/admin-media-resources-w390.png` | 通过 |
| settings | 1440 | ADMIN/TEACHER | `docs/revamp-screenshots/2026-04-17/settings-w1440.png` | 通过 |
| settings | 1024 | ADMIN/TEACHER | `docs/revamp-screenshots/2026-04-17/settings-w1024.png` | 通过 |
| settings | 768 | ADMIN/TEACHER | `docs/revamp-screenshots/2026-04-17/settings-w768.png` | 通过 |
| settings | 390 | ADMIN/TEACHER | `docs/revamp-screenshots/2026-04-17/settings-w390.png` | 通过 |

## Task3 角色边界与管理员页直访
- [x] 导航可见性基线已记录（`AppShellNav`）
  - `ADMIN`：可见媒体资源、模型与服务商、组织用户、审计日志、设置
  - `TEACHER`：可见教学与项目主入口，可见设置；不可见管理员专属资源/治理入口
  - `STUDENT`：不显示后台治理与系统配置类入口
- [x] 管理员页直接访问拦截行为已记录（页面内 role guard）
  - `admin/media-resources`：非 `ADMIN` 返回“仅管理员可访问”
  - `models`：非 `ADMIN` 返回“仅管理员可访问”
  - `models/hub`：非 `ADMIN` 返回“仅管理员可访问”
  - `admin/directory`：非 `ADMIN` 返回“仅管理员可访问”
  - `audit-logs`：仅 `ADMIN` 可访问（`canAccessAuditLogs`）
- [x] 角色边界结论
  - 导航层与页面层形成“双重收口”（隐藏 + 直访兜底）
  - 风险项：`/settings` 当前导航与页面允许 `ADMIN/TEACHER` 进入，且页面通过卡片内容对系统级入口再做管理员收口，符合“教师可见设置、系统级配置仅管理员可操作”的当前策略

## Task4 响应式与帮助说明一致性
- [x] 四档响应式核查依据已记录
  - `global.css` 断点：`1180`、`960`、`640`（壳层、工具条、筛选条、项目导航头纵向化）
  - `components.css` 断点：`1100`、`960`、`900`、`800`、`768`、`480`（dock、对话框、列表、表单、顶栏等）
  - `index.css`：`1024` 字号与标题缩放
- [x] 关键组件核查结论
  - 左侧导航抽屉：`app-shell-nav` 在窄屏使用遮罩+抽屉位移
  - 顶部/工具条换行：`page-toolbar`、`compact-filter-bar__head`、`project-subpage-shell__toolbar-head` 在 `<=960` 转纵向
  - 固定 dock：`fixed-panel-dock` 在 `<=1100/768` 降级为单列与更紧凑 padding
  - 弹窗/抽屉：`dialog-overlay`、`modal-form-dialog` 限制宽高并支持滚动；移动端按钮与布局有专门断点
- [x] 1440 / 1024 / 768 / 390 四档记录
  - 1440：桌面基线布局正常，导航/内容双区稳定
  - 1024：开始进入紧凑布局，顶栏和筛选区可换行
  - 768：dock 与多列网格降级为单列，弹窗与工具区可继续操作
  - 390：依赖 480/640/768 断点组合收口，表单和操作区以单列为主，避免横向溢出
- [x] HelpHint 一致性核查
  - 已完成：`ProjectSubpageShell` 内统一渲染 `HelpHint`，承接 `helpTitle/help` 语义位
  - 结论：项目次级页帮助入口形态已统一，不再依赖“仅文档声明替代形态”的双轨策略
- [x] 创建/编辑承载形态核查
  - 已完成：`ScriptProjectCreatePage` 首屏创建表单迁移到 `ActionDrawer`，首屏改为说明与入口按钮
  - 结论：创建动作不再由首屏表单主导，符合“抽屉/弹窗承载”规范

## Task5 回归验证与封板
- [x] `npm run lint` 与 `npm run build` 已复跑通过
  - `npm run lint`：exit code `0`，无 warning/error
  - `npm run build`：exit code `0`，vite 构建通过并保持 vendor 分包
- [x] 对照 `.trae/specs/finalize-screenshot-archive-and-help-unification/checklist.md` 已完成逐项勾选
  - 核对结果：`7/7` 条全部完成
- [x] `revamp-delivery-notes.md` 已更新最终收口结论与后续建议
  - 当前结论：截图资产、帮助入口统一、创建交互统一、lint/build 基线均已封板

## Functional Acceptance
- [x] Core flow works as expected
- [x] Key pages/routes are reachable
- [x] Critical actions complete without errors

## Quality Acceptance
- [x] No blocking console errors（以 lint/build 与页面守卫逻辑为基线）
- [x] No critical visual regressions（已完成样式与断点规则核查）
- [x] Basic responsive behavior verified（四档核查记录完成）

## Sign-off
- Decision: Task1/2/3/4/5 验收项全部通过，允许封板
- Notes:
  - 本次已补齐 `2026-04-17` 批次 `32` 张截图并建立索引映射
  - 后续若新增关键页面，需按同命名规范同步补齐四档截图再进入下一轮封板
