# Tasks
- [x] Task 1: 盘点并补齐 V2 角色与视频数据接入边界。
  - [x] SubTask 1.1: 梳理 `aigc-cartoon-react` 现有 V2 页面、接口封装与类型定义，确认角色和视频能力的复用边界
  - [x] SubTask 1.2: 核对 `aigc-cartoon-server` 角色与视频接口字段，明确前端表单最小必填项与展示字段
  - [x] SubTask 1.3: 如发现接口返回结构或字段名不满足前端接入，做最小兼容性调整方案

- [x] Task 2: 实现 `/v2/assets` 资产库角色浏览页。
  - [x] SubTask 2.1: 增加项目选择或项目筛选入口，支持切换查看不同项目的角色资产
  - [x] SubTask 2.2: 展示角色列表、空状态、加载态与失败重试
  - [x] SubTask 2.3: 增加到项目工作台的跳转入口，形成浏览与编辑分工

- [x] Task 3: 完成项目工作台"资产库"Tab 的角色完整管理。
  - [x] SubTask 3.1: 增加角色新增和编辑表单，支持名称、描述、形象提示词、语音配置等基础字段
  - [x] SubTask 3.2: 接入角色图片上传与预览，保存上传结果到角色数据
  - [x] SubTask 3.3: 增加角色删除操作，并在成功后刷新项目统计和角色列表

- [x] Task 4: 在 V2 工作台补充项目视频记录基础管理。
  - [x] SubTask 4.1: 新增视频接口封装和前端类型，接入项目视频列表查询
  - [x] SubTask 4.2: 在工作台增加视频记录展示区，展示状态、时长、缩略图、视频地址和关联分镜
  - [x] SubTask 4.3: 提供视频记录创建表单，支持提交最小必要字段并在成功后刷新列表

- [ ] Task 5: 做 V2 资产与视频闭环回归验证。
  - [x] SubTask 5.1: 验证 `/v2/assets`、`/v2/projects/:id` 的角色浏览和编辑流程可用
  - [x] SubTask 5.2: 验证角色图片上传、删除、空状态和失败提示表现正常
  - [x] SubTask 5.3: 验证视频记录列表与创建流程不影响现有项目、配置、剧本和分镜能力
  - [x] SubTask 5.4: 运行前端构建或类型检查，并补充本次改动直接相关的最小必要测试或诊断

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 1
- Task 5 depends on Task 2, Task 3, Task 4