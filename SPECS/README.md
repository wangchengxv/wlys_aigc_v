# SPECS - 需求规格索引

## 1. 说明
`SPECS` 是本项目的需求契约层，所有开发任务都必须映射到一个或多个规格文件。

## 2. 文件导航
- `product-scope.md`：产品范围、角色、核心流程与阶段目标。
- `functional-modules.md`：功能模块明细与关键交互规则。
- `nonfunctional-security.md`：性能、安全、可扩展、可用性约束。
- `api-contracts.md`：核心接口契约、请求响应、状态语义。

## 3. 使用规则
- 新需求进入开发前，先在 `SPECS` 落条目。
- 涉及接口或字段变化时，先改 `api-contracts.md`，再改实现。
- 验收必须引用具体规格条目，避免“感觉正确”。
