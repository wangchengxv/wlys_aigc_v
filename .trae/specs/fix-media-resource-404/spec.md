# 修复媒体资源中心资源打开404问题 Spec

## Why
当前媒体资源中心可以正常展示资源列表，但用户点击"打开"按钮时跳转到404页面。问题根源在于 `FileAssetController.preview` 查找文件的逻辑与 `MediaResourceService.listRecent` 返回数据的来源不一致：`listRecent` 从数据库直接查询所有 `StoredFileRecord`，但 `preview` 只在 `ScriptProjectAggregate.files` 内查找文件，导致大量文件记录无法被正确定位。

## What Changes
- 修改 `FileAssetController.preview` 和 `download` 方法，直接通过 `StoredFileRecordRepository` 查询文件记录，而非依赖 `ScriptProjectAggregate` 间接查找
- 为 `StoredFileRecord` 实体添加数据库索引以提升按 `fileId` 查询的性能
- 确保 `publicUrl` 正确生成并可访问

## Impact
- Affected specs: 媒体资源中心文件预览与下载功能
- Affected code:
  - `aigc-server/src/main/java/com/example/aigc/controller/FileAssetController.java`
  - `aigc-server/src/main/java/com/example/aigc/entity/StoredFileRecord.java`
  - `aigc-server/src/main/java/com/example/aigc/repository/StoredFileRecordRepository.java`
  - `aigc-server/src/main/java/com/example/aigc/repository/jpa/JpaStoredFileRecordRepository.java`

## ADDED Requirements
### Requirement: 文件预览/下载接口直接查询数据库
`FileAssetController` 的 `/api/v1/files/{fileId}` 预览和 `/api/v1/files/{fileId}/download` 下载接口 SHALL 直接通过 `StoredFileRecordRepository` 根据 `fileId` 查询文件记录，而非通过项目聚合间接查找。

#### Scenario: 成功预览媒体资源
- **WHEN** 用户在媒体资源中心点击"打开"按钮
- **THEN** 系统通过 `fileId` 直接查询 `StoredFileRecord` 表，获取文件元数据
- **AND** 文件存在时返回正确的预览响应
- **AND** 文件不存在时返回404

#### Scenario: 成功下载媒体资源
- **WHEN** 用户在媒体资源中心点击"下载"按钮
- **THEN** 系统通过 `fileId` 直接查询 `StoredFileRecord` 表，获取文件元数据
- **AND** 文件存在时返回正确的下载响应（带 Content-Disposition attachment）
- **AND** 文件不存在时返回404

### Requirement: 数据库索引优化
`StoredFileRecord` 表 SHALL 在 `file_id` 字段上建立唯一索引（已有主键约束），并考虑在 `project_id` 和 `created_at` 字段上建立复合索引以优化按项目查询和按时间排序查询。

#### Scenario: 索引已建立
- **WHEN** 数据库迁移执行后
- **THEN** `stored_file_record` 表在 `file_id` 上有主键约束（唯一索引）
- **AND** 查询性能满足媒体资源列表加载要求

## MODIFIED Requirements
### Requirement: StoredFileRecord 持久化完整性
`StoredFileRecord` 作为所有媒体资产的元数据记录，SHALL 在文件存储时同步持久化到数据库，确保 `MediaResourceService.listRecent` 查询结果与实际存储文件一致。

#### Scenario: 文件元数据正确持久化
- **WHEN** 任意媒体文件通过 `LocalMediaStorageService` 存储时
- **THEN** 对应的 `StoredFileRecord` 元数据记录被正确保存到数据库
- **AND** 可以在 `MediaResourceService.listRecent` 中被查询到
- **AND** 可以通过 `FileAssetController.preview` 正确预览

## REMOVED Requirements
### Requirement: 无

## Technical Notes
1. **问题根因分析**:
   - `MediaResourceService.listRecent()` 调用 `storedFileRecordRepository.findRecent(200)` 从数据库直接获取所有最近200条文件记录
   - `FileAssetController.preview()` 调用 `scriptProjectService.findFile(aggregate, fileId)` 在项目聚合内查找文件
   - `findFile` 方法只在 `aggregate.files` 列表中遍历查找，该列表来自 `JpaScriptProjectRepository.findById` 时加载的 `fileRepository.findAllByProjectId(projectId)`
   - 当 `MediaResourceService` 查询的文件不在任何项目聚合中时，`findFile` 返回null，导致404

2. **修复方案**:
   - 在 `JpaStoredFileRecordRepository` 中添加 `findByFileId(String fileId)` 方法
   - 修改 `FileAssetController.preview/download` 直接注入 `StoredFileRecordRepository` 并使用 `findByFileId` 查询
   - 移除对 `scriptProjectService.findFile` 的依赖

3. **索引检查**:
   - `file_id` 已通过 `@Id` 注解自动成为主键，无需额外迁移
   - 确认 `JpaStoredFileRecordRepository.findRecent` 的查询已有合适索引支持
