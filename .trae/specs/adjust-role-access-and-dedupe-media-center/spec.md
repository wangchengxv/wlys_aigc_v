# 角色访问修正与媒体资源中心去重 Spec

## Why
当前角色访问与页面展示存在偏差：学生侧三视图工具访问不稳定、教师侧媒体资源入口不可用，且媒体资源中心存在重复栏目，影响可用性与验收一致性。

## What Changes
- 调整三视图工具能力边界，确保学生可在学生界面访问并使用。
- 调整媒体资源能力边界，确保老师可在老师界面访问并使用。
- 清理媒体资源中心重复栏目，仅保留单一且清晰的信息区块。
- 保持模型配置与服务商中心的管理员专属边界不变。

## Impact
- Affected specs: 角色访问策略、媒体资源中心页面展示一致性
- Affected code: `aigc-site-react/src/components` 导航与角色过滤逻辑、`aigc-site-react/src/pages/MediaResourcesPage.tsx`、相关路由门禁与菜单配置

## ADDED Requirements
### Requirement: 学生可访问三视图工具
系统 SHALL 在学生角色登录后，允许其在学生界面访问并使用三视图工具页面及核心操作。

#### Scenario: 学生访问三视图成功
- **WHEN** 学生账号登录并进入工具导航
- **THEN** 可见并进入三视图工具页面，且页面核心功能可正常使用

### Requirement: 老师可访问媒体资源功能
系统 SHALL 在老师角色登录后，允许其在老师界面访问并使用媒体资源功能。

#### Scenario: 老师访问媒体资源成功
- **WHEN** 老师账号登录并进入资源相关导航
- **THEN** 可见并进入媒体资源页面，且页面核心功能可正常使用

## MODIFIED Requirements
### Requirement: 媒体资源中心页面信息区块唯一
媒体资源中心页面 SHALL 不出现重复栏目（如重复标题、重复说明或重复统计区块），首屏信息结构保持单一、清晰。

#### Scenario: 媒体资源中心去重后展示
- **WHEN** 任意有权限角色进入媒体资源中心
- **THEN** 页面仅展示一套媒体资源中心主信息区块，不出现重复内容

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次变更不删除既有功能，仅修正权限与展示一致性问题。
**Migration**: 无需迁移，按新权限与页面结构直接生效。
