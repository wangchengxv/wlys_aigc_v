# Tasks
- [x] Task 1: 盘点项目全景信息（目标、技术栈、目录职责、运行方式），形成 AI 可读的“项目一页纸”。
  - [x] SubTask 1.1: 扫描关键目录与入口文件，明确前端主应用与支撑脚本边界
  - [x] SubTask 1.2: 识别核心页面（含 StoryboardLite 相关页面）与关键交互流程
  - [x] SubTask 1.3: 汇总依赖与外部系统接入点（API、模型调用、存储、配置）

- [x] Task 2: 构建“AI 改动前工程理解模板”，约束先分析后实现。
  - [x] SubTask 2.1: 定义固定输出结构（宏观目标、模块映射、影响面、风险、验证）
  - [x] SubTask 2.2: 增加“禁止盲改”规则与触发条件说明
  - [x] SubTask 2.3: 提供任务接入示例，保证不同需求可复用同一模板

- [x] Task 3: 建立实现细节索引，支持 AI 快速定位但不越权改动。
  - [x] SubTask 3.1: 整理状态管理、组件复用、数据请求与副作用处理的关键位置
  - [x] SubTask 3.2: 标记高风险改动区（跨页面共享逻辑、配置项、公共方法）
  - [x] SubTask 3.3: 定义最小验证路径（静态检查、关键流程手测、回归关注点）

- [x] Task 4: 输出最终交付说明，供后续每次任务启动前复用。
  - [x] SubTask 4.1: 给出“本次任务先做什么”的标准步骤
  - [x] SubTask 4.2: 给出“何时可以开始改代码”的准入条件
  - [x] SubTask 4.3: 给出“完成后如何自证正确”的验证口径

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 2, Task 3
