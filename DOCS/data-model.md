# Data Model

## 1. 核心表
- `user`：用户身份与状态。
- `project`：项目基础信息和风格配置。
- `script` / `script_episode`：剧本与剧集结构。
- `subject`：角色/场景/道具主体。
- `storyboard`：镜头级分镜信息。
- `subject_storyboard_rel`：主体与分镜关联。
- `asset`：统一资产中心。
- `ai_task`：异步任务状态机。
- `ai_model`：模型能力配置。

## 2. 关键关系
- 用户 `1:N` 项目。
- 项目 `1:N` 剧本、主体、分镜、资产、任务。
- 分镜 `N:N` 主体（通过关联表）。
- 分镜图片/视频通过资产 ID 回链资产中心。

## 3. 设计约束
- 全局建议字段：`id/create_time/update_time/deleted`。
- 业务删除优先逻辑删除。
- 任务表记录请求参数、结果摘要、错误信息、重试次数。
- 资产表统一记录类型、存储 key、尺寸/时长、来源。
