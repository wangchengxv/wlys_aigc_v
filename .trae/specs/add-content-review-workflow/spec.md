# 内容审核流与驳回机制 Spec

## Why
当前仓库已经具备镜头视频、配音、口型同步、项目级成片与导出包交付能力，但交付物一旦生成后，仍缺少“提交审核、审核通过、审核驳回、按意见重提”的治理闭环。没有内容审核流后，教师、管理员和平台侧无法对最终交付物形成明确的准入控制与留痕管理。

## What Changes
- 为剧本工程新增内容审核工作流，支持提交审核、通过、驳回、重提
- 新增审核任务 / 审核记录数据模型，记录审核状态、审核人、审核意见、驳回原因与重提次数
- 在生产编排与项目状态中增加“待审核 / 审核通过 / 审核驳回”状态聚合
- 前端新增最小可用审核操作区，使教师或管理员可以处理审核，学生或创作者可以查看结果并重提
- 审计日志接入审核关键动作，确保提审、通过、驳回、重提都有过程留痕
- 本次不实现复杂规则引擎、敏感内容自动识别、多级会签与消息通知，但要求保留后续扩展点

## Impact
- Affected specs: 剧本工程、项目级成片、导出包交付、审计日志、权限模型、导出页
- Affected code: `aigc-server/src/main/java/com/example/aigc/service/ScriptProductionOrchestrator.java`、`aigc-server/src/main/java/com/example/aigc/service/AuthorizationService.java`、`aigc-server/src/main/java/com/example/aigc/controller/VideoPipelineController.java`、`aigc-server/src/main/java/com/example/aigc/service/AuditLogService.java`、`aigc-site-react/src/pages/ScriptProjectExportPage.tsx`、`aigc-site-react/src/stores/scriptProjectStore.ts`、`aigc-site-react/src/api/index.ts`

## ADDED Requirements
### Requirement: 剧本工程必须支持提交审核
系统 SHALL 为剧本工程提供项目级提交审核能力，使创作者在导出包准备完成后可以正式提交审核。

#### Scenario: 用户提交项目审核
- **WHEN** 用户在项目导出页选择提交审核
- **AND** 当前项目已经存在可用导出包
- **THEN** 系统创建或更新项目审核记录
- **AND** 项目状态进入“待审核”

### Requirement: 审核流必须支持通过与驳回
系统 SHALL 支持审核人对项目执行通过或驳回操作，并记录审核意见。

#### Scenario: 审核人驳回项目
- **WHEN** 审核人在审核区选择驳回
- **THEN** 系统记录驳回结论、审核意见与审核时间
- **AND** 项目状态进入“审核驳回”
- **AND** 创作者可以看到驳回原因

### Requirement: 审核驳回后必须支持重提
系统 SHALL 在项目被驳回后允许创作者重新提交审核，以形成最小闭环。

#### Scenario: 创作者根据意见修改后重提
- **WHEN** 项目已处于“审核驳回”
- **AND** 创作者完成修正并再次提交
- **THEN** 系统保留历史审核记录
- **AND** 当前审核状态重新进入“待审核”
- **AND** 重提次数会被记录

### Requirement: 审核动作必须有权限约束
系统 SHALL 区分“提交审核”和“处理审核”的角色权限，避免普通用户越权通过自己的交付物。

#### Scenario: 普通创作者尝试审核自己的项目
- **WHEN** 没有审核权限的用户调用通过或驳回接口
- **THEN** 系统拒绝该操作
- **AND** 返回明确的权限错误

### Requirement: 审核关键动作必须写入审计日志
系统 SHALL 将提审、审核通过、审核驳回、重提等关键动作记录到统一审计日志中。

#### Scenario: 审核通过
- **WHEN** 审核人通过某个项目
- **THEN** 系统写入审计日志
- **AND** 日志至少包含项目 ID、操作人、操作类型与审核结论摘要

### Requirement: 前端必须提供最小可用审核操作区
系统 SHALL 在导出页或项目交付页提供审核状态展示与操作入口，使创作者和审核人都能完成最小审核闭环。

#### Scenario: 用户打开项目导出页
- **WHEN** 用户进入项目导出页
- **THEN** 页面展示当前审核状态、最近审核意见与可用操作
- **AND** 创作者在可提交时可以发起提审或重提
- **AND** 审核人可以执行通过或驳回

## MODIFIED Requirements
### Requirement: 生产闭环必须从“交付包可生成”升级为“交付包可审核”
系统 SHALL 在不破坏现有导出包能力的前提下，将生产闭环从“可生成交付物”扩展为“可提交审核并形成审核结论”，为后续治理与留档提供基础。

#### Scenario: 项目进入最终交付阶段
- **WHEN** 用户已生成项目级导出包
- **THEN** 系统允许项目进入审核流程
- **AND** 项目状态可以区分“交付包已就绪但未提审”“待审核”“审核通过”“审核驳回”

### Requirement: 导出页必须展示审核状态
系统 SHALL 在现有导出页中增加审核状态提示与审核操作区，使用户明确项目当前是否可对外交付。

#### Scenario: 用户进入导出页
- **WHEN** 项目尚未审核通过
- **THEN** 页面提示当前审核状态
- **AND** 对未通过项目显示提审、重提或查看驳回意见的入口

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为新增生产治理能力，不移除现有能力。
**Migration**: 无需迁移。
