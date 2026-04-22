# Tasks

- [x] Task 1: 梳理并落地剧本工程流程顺序，新增“全局设定”为第 1 步
  - [x] SubTask 1.1: 盘点当前流程步骤定义与侧边栏渲染来源
  - [x] SubTask 1.2: 调整步骤顺序与标签，确保“全局设定”在首位
  - [x] SubTask 1.3: 保证当前步骤高亮、状态展示与新顺序一致

- [x] Task 2: 调整“新建剧本工程”成功后的默认跳转到全局设定
  - [x] SubTask 2.1: 定位创建成功后的跳转逻辑
  - [x] SubTask 2.2: 更新默认落点路由为全局设定步骤
  - [x] SubTask 2.3: 验证管理员/教师等主要角色路径一致性

- [x] Task 3: 建立单页流程化切换体验（顺序按钮 + 侧边栏）
  - [x] SubTask 3.1: 在工作区统一容器中提供“上一步/下一步”操作
  - [x] SubTask 3.2: 保证顺序切换、侧边栏点击、URL 路由三者同步
  - [x] SubTask 3.3: 优化小屏场景下的可用性（避免导航遮挡内容）

- [x] Task 4: 增加全局设定未完成时的后续步骤引导与限制提示
  - [x] SubTask 4.1: 定义最小可通过的全局设定完成条件
  - [x] SubTask 4.2: 在后续关键步骤增加缺失项提示与返回入口
  - [x] SubTask 4.3: 保证提示不影响已有数据保存与读取

- [x] Task 5: 完成回归验证与交付检查
  - [x] SubTask 5.1: 手动验证创建后首步落点、侧边栏顺序、顺序跳转
  - [x] SubTask 5.2: 运行必要的前端检查（类型/构建或相关测试）
  - [x] SubTask 5.3: 补齐并勾选 checklist.md 的所有检查项

- [x] Task 6: 修复前端构建阻断项并完成复验
  - [x] SubTask 6.1: 修复 `AdminDirectoryPage.tsx` 中未使用导入导致的 TS6133
  - [x] SubTask 6.2: 重新执行前端构建验证并确认通过
  - [x] SubTask 6.3: 回填 Task 5 与 checklist.md 的剩余项

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 1 and Task 3
- Task 5 depends on Task 2, Task 3, and Task 4
- Task 6 depends on Task 5
