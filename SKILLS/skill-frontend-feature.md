# Skill: Frontend Feature

## 输入
- 页面模块、交互目标、接口依赖、状态要求。

## 执行步骤
1. 映射路由与页面域（工作台子模块优先）。
2. UI 与业务状态分离：组件负责编排，`api/` 负责请求。
3. 使用 TanStack Query 管理加载、重试、轮询。
4. 对 AI 任务按钮实现 loading/disabled/failed/retry 全状态。
5. 补组件测试与关键 E2E 流程。

## 输出
- 页面交互清单。
- 异常与空状态覆盖结论。
- 可回归测试建议。
