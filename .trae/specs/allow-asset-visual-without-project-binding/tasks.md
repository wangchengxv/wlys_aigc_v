# Tasks
- [x] Task 1: 移除三视图工具“必须绑定工程”阻断逻辑
  - [x] SubTask 1.1: 定位三视图入口、页面初始化、提交链路中的绑定工程强校验点
  - [x] SubTask 1.2: 将阻断校验改为非阻断提示，确保未绑定工程可进入并使用核心功能
  - [x] SubTask 1.3: 清理或调整相关文案，避免继续表达“必须先绑定工程”

- [x] Task 2: 统一三视图工具角色访问范围为学生/老师/管理员
  - [x] SubTask 2.1: 更新菜单与入口角色可见性配置
  - [x] SubTask 2.2: 更新路由门禁和页面级角色校验，确保“可见即可达”
  - [x] SubTask 2.3: 回归验证三类角色访问三视图一致性，不影响其他权限边界

- [x] Task 3: 验证与收尾
  - [x] SubTask 3.1: 执行前端关键流程验证（未绑定工程使用、三角色访问）
  - [x] SubTask 3.2: 运行必要构建/静态检查，确认无新增报错
  - [x] SubTask 3.3: 完成 checklist 验收项勾选

# Task Dependencies
- Task 2 depends on Task 1（先去除使用阻断，再统一角色访问以便联调）
- Task 3 depends on Task 1 and Task 2
