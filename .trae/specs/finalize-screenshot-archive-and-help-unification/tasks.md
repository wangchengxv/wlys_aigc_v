# Tasks
- [x] Task 1: 建立截图归档目录与清单结构
  - [x] 在 docs 下创建本轮验收截图归档目录（含页面与视口命名规则）
  - [x] 在验收文档中补充截图索引表字段（页面、视口、角色、文件路径、结果）
  - [x] 明确最小截图覆盖范围与缺失补拍规则

- [x] Task 2: 完成四档截图采集与文档回填
  - [x] 采集关键页面在 1440/1024/768/390 四档截图并归档
  - [x] 在 `revamp-acceptance-checklist.md` 回填截图路径与结论
  - [x] 对截图中发现的明显布局问题登记并修复

- [x] Task 3: 完成 HelpHint 全量统一与替代规则收口
  - [x] 盘点项目链路与后台关键页面的帮助说明入口
  - [x] 将可统一页面落地为 HelpHint（或统一壳层封装）
  - [x] 对无法直接迁移页面给出统一替代规范并更新文档

- [x] Task 4: 完成创建/编辑抽屉弹窗全量统一
  - [x] 逐页核查创建/编辑动作承载方式（抽屉/弹窗/首屏表单）
  - [x] 将不符合规范的首屏表单改造为抽屉或弹窗
  - [x] 复核操作链路可达性与移动端可用性

- [x] Task 5: 回归验证与封板
  - [x] 运行 `npm run lint` 与 `npm run build`
  - [x] 对照 checklist.md 完成逐项勾选
  - [x] 更新 `revamp-delivery-notes.md` 收口结论与遗留项

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 1
- Task 5 depends on Task 2, Task 3, and Task 4
