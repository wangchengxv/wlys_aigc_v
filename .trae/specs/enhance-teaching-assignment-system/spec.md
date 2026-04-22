# 教学作业系统功能完善 Spec

## Why
当前系统已有基础的教学作业发布、提交和评分功能，但缺少教师端高效批量评分、学生作品预览、成绩导出等关键能力，需要完善以满足实际教学场景需求。

## What Changes

### 后端增强
- 新增批量评分接口，支持教师一键批量提交评分结果
- 新增作业统计接口，返回提交率、平均分、分布等聚合数据
- 新增成绩导出接口，生成包含学生信息、作品链接、评分结果的 CSV 文件
- 学生提交时可附带简短说明（已有支持）

### 前端增强
- 教师评分页新增批量评分模式，支持勾选多个提交后一键打分
- 学生提交卡片新增作品预览区（显示项目封面/视频缩略图）
- 评分操作区优化分数输入体验（滑块/快捷分数按钮）
- 新增成绩统计面板（提交率、平均分、分数段分布）
- 新增成绩导出功能（导出为 CSV）

## Impact
- Affected specs: 课程与实训、作业管理、提交评分
- Affected code:
  - `aigc-server/src/main/java/com/example/aigc/service/TeachingSubmissionService.java`
  - `aigc-server/src/main/java/com/example/aigc/controller/TeachingSubmissionController.java`
  - `aigc-site-react/src/pages/TeachingAssignmentDetailPage.tsx`
  - `aigc-site-react/src/api/index.ts`

## ADDED Requirements

### Requirement: 批量评分
系统 SHALL 支持教师批量选择多个学生提交并一次性提交评分结果。

#### Scenario: 批量评分成功
- **WHEN** 教师在作业评分页勾选多个提交记录，设置统一的分数和评语
- **THEN** 系统批量更新所有选中提交的评分状态和分数
- **AND** 返回成功更新的数量和失败列表

### Requirement: 作业统计
系统 SHALL 提供作业维度的统计信息，包括已提交数、待评分数、平均分等。

#### Scenario: 获取统计数据
- **WHEN** 教师打开作业评分页面
- **THEN** 页面顶部显示提交总数、已评分数、平均分等统计数据

### Requirement: 成绩导出
系统 SHALL 支持将作业评分结果导出为 CSV 文件。

#### Scenario: 导出成绩
- **WHEN** 教师点击"导出成绩"按钮
- **THEN** 浏览器下载包含学生姓名、班级、项目名、提交时间、分数、评语的 CSV 文件

### Requirement: 作品预览
系统 SHALL 在学生提交卡片中显示关联项目的封面或视频缩略图。

#### Scenario: 查看作品
- **WHEN** 教师查看学生提交列表
- **THEN** 每个提交卡片显示该学生项目的视频封面或图片预览

## MODIFIED Requirements

### Requirement: 评分保存
**原有的评分功能保持不变，新增强制批量操作入口：**

系统 SHALL 支持单个评分保存和批量评分两种模式。

#### Scenario: 单个评分
- **WHEN** 教师填写单个提交的分数和评语后点击保存
- **THEN** 仅更新该提交的评分结果

#### Scenario: 批量评分
- **WHEN** 教师勾选多个提交，设置统一分数，点击"批量评分"按钮
- **THEN** 批量更新所有选中提交的评分状态和分数
- **AND** 显示"成功更新 N 条"提示

## REMOVED Requirements
无