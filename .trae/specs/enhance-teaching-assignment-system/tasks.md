# Tasks

- [x] Task 1: 后端 - 新增批量评分接口
  - [x] SubTask 1.1: 在 TeachingSubmissionService 添加批量评分方法
  - [x] SubTask 1.2: 在 TeachingSubmissionController 添加批量评分端点
  - [x] SubTask 1.3: 新增批量评分请求/响应类型

- [x] Task 2: 后端 - 新增作业统计接口
  - [x] SubTask 2.1: 在 TeachingSubmissionService 添加获取统计信息方法
  - [x] SubTask 2.2: 在 TeachingSubmissionController 添加统计端点

- [x] Task 3: 后端 - 新增成绩导出接口
  - [x] SubTask 3.1: 在 TeachingSubmissionService 添加导出 CSV 方法
  - [x] SubTask 3.2: 在 TeachingSubmissionController 添加导出端点

- [x] Task 4: 前端 - 新增批量评分 UI
  - [x] SubTask 4.1: 添加批量选择 Checkbox 和批量操作工具栏
  - [x] SubTask 4.2: 添加快捷分数按钮（60/70/80/90/100）
  - [x] SubTask 4.3: 实现批量评分提交逻辑

- [x] Task 5: 前端 - 新增成绩统计面板
  - [x] SubTask 5.1: 调用统计 API 获取数据
  - [x] SubTask 5.2: 渲染提交率、平均分、分数分布

- [x] Task 6: 前端 - 新增成绩导出功能
  - [x] SubTask 6.1: 添加导出按钮
  - [x] SubTask 6.2: 实现 CSV 下载逻辑

- [x] Task 7: 前端 - 新增作品预览区
  - [x] SubTask 7.1: 获取项目封面/缩略图
  - [x] SubTask 7.2: 在提交卡片中显示预览

- [x] Task 8: 前端 - 新增 API 接口
  - [x] SubTask 8.1: 添加 getAssignmentStats 接口
  - [x] SubTask 8.2: 添加 batchReviewSubmissions 接口
  - [x] SubTask 8.3: 添加 exportAssignmentGrades 接口

# Task Dependencies
- Task 2 依赖 Task 1（统计接口需要类似的返回结构）
- Task 5 依赖 Task 2 和 Task 7（前端需要后端统计数据和项目信息）
- Task 6 依赖 Task 3（导出功能需要后端支持）
- Task 4 依赖 Task 1（批量评分需要后端接口）
- Task 8 依赖 Task 1, 2, 3（前端 API 对应后端接口）
