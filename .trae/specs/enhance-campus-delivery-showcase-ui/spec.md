# 高校 AIGC 平台展示与交付体验增强 Spec

## Why
当前平台已经完成后台化改版与第二阶段 UI 精修，但首页展示面、项目详情页和导出交付页仍偏“功能页”，缺少更强的展示感、成果感和可视化表达。
本次继续推进下一步，重点增强平台的产品展示能力和项目交付体验，让平台既适合高校日常后台操作，也适合对外演示与成果汇报。

## What Changes
- 强化首页展示面，在保留平台入口功能的基础上增加更有说服力的价值展示与可视化信息模块
- 升级平台概览页的数据可视化表达，引入更直观的图表式或可视化卡片式统计模块
- 重构项目详情页的信息结构，突出项目状态、成果封面、生产进度、审核状态和交付 readiness
- 重构成片与导出页的交付体验，使用户更清楚地理解当前项目距离“可提交、可归档、可交付”还差哪些环节
- 提升项目详情、导出页与首页中的“作品感”“成果感”“平台专业感”，让界面更适合高校演示、答辩和汇报场景
- 保持现有角色边界、业务流程和路由逻辑不变，仅增强展示质量、信息层级和交互表达

## Impact
- Affected specs: `revamp-campus-aigc-admin-console`, `polish-campus-aigc-admin-ui`, `add-export-package-delivery`, `add-content-review-workflow`
- Affected code: `aigc-site-react/src/pages/HomePage.tsx`, `aigc-site-react/src/components/home/*`, `aigc-site-react/src/pages/OperationsDashboardPage.tsx`, `aigc-site-react/src/pages/ScriptProjectDetailPage.tsx`, `aigc-site-react/src/pages/ScriptProjectExportPage.tsx`, `aigc-site-react/src/components/script/*`, `aigc-site-react/src/styles/*`

## ADDED Requirements
### Requirement: 首页具备更完整的平台展示面
系统 SHALL 为首页提供更完整的平台展示区，使高校用户在进入系统时能够快速理解平台定位、核心能力与成果价值，而不是只看到普通跳转页。

#### Scenario: 访问首页
- **WHEN** 用户进入首页
- **THEN** 页面展示平台定位、核心价值、关键能力和场景化介绍模块
- **THEN** 页面具备更强的品牌感和产品感，适合作为平台门户与演示首页

### Requirement: 平台概览页具备更直观的数据可视化模块
系统 SHALL 在平台概览页中提供比纯列表或纯指标卡更直观的数据可视化表达，帮助教师和管理员快速理解教学、创作与交付状态。

#### Scenario: 查看平台概览
- **WHEN** 教师或管理员进入平台概览页
- **THEN** 页面展示教学运营、项目状态或审核进度的可视化模块
- **THEN** 可视化模块与现有指标卡、待办和活动区风格一致

### Requirement: 项目详情页突出成果展示与治理上下文
系统 SHALL 将项目详情页提升为“项目概览中心”，不仅展示字段，还应突出项目封面、当前阶段、关键里程碑、治理状态和后续可执行入口。

#### Scenario: 查看项目详情
- **WHEN** 用户进入某个项目详情页
- **THEN** 页面优先展示项目名称、封面、摘要、课程归属、提交人、审核状态和交付状态
- **THEN** 页面以更清晰的结构呈现基础信息、资产统计、生产进度和治理入口
- **THEN** 关键入口如预览、资产、视频、导出与审计具有更清晰的主次关系

### Requirement: 导出页体现交付 readiness 与成果下载中心
系统 SHALL 将成片与导出页升级为交付中心，使用户能一眼理解成片、审核、导出包和最终提交之间的关系。

#### Scenario: 查看导出页
- **WHEN** 用户进入成片与导出页
- **THEN** 页面优先展示项目交付 readiness、当前缺口、已完成项和最终下载入口
- **THEN** 页面突出成片预览、导出包下载、审核记录和交付说明
- **THEN** 用户可以快速判断当前项目是否已经达到“可交付”状态

### Requirement: 展示增强不破坏现有业务链路
系统 SHALL 在增强首页、概览页、项目详情页和导出页展示效果时，保持原有角色边界、业务能力和主要路由跳转不变。

#### Scenario: 完成界面增强后使用平台
- **WHEN** 用户继续按原有方式浏览平台、进入项目详情、执行导出或审核
- **THEN** 业务流程、权限边界和关键操作保持原有逻辑
- **THEN** 新增展示层不会造成功能中断或导航混乱

## MODIFIED Requirements
### Requirement: 项目详情与导出页从流程页升级为成果页
现有项目详情页和导出页 SHALL 在保留流程操作能力的基础上，进一步承担“项目展示、成果汇报、交付状态说明”的页面职责。

#### Scenario: 使用项目治理相关页面
- **WHEN** 用户从项目列表进入详情或导出页
- **THEN** 用户看到的不只是操作按钮和字段，还包括项目成果概览、交付 readiness 和更完整的展示信息

### Requirement: 首页从入口页升级为品牌与能力展示页
现有首页 SHALL 在保留进入工作台和服务商入口的前提下，继续增强品牌表达、能力分区和平台场景说明。

#### Scenario: 首次访问平台
- **WHEN** 用户进入首页
- **THEN** 页面更适合作为对外展示和校内宣传使用，而不是单纯功能跳转页

## REMOVED Requirements
### Requirement: 项目交付状态仅通过零散说明文本表达
**Reason**: 交付准备度是高校答辩、课程验收和作品归档场景中的关键判断信息，仅靠零散文本不利于快速理解。
**Migration**: 统一通过交付 readiness 模块、成果概览区、下载区和审核区展示项目当前交付完成度与后续动作。
