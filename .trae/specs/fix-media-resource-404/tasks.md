# Tasks
- [x] Task 1: 添加JpaStoredFileRecordRepository的findByFileId方法
  - [x] SubTask 1.1: 在JpaStoredFileRecordRepository中添加findByFileId(String fileId)方法
  - [x] SubTask 1.2: 在SpringDataRepositories中添加对应的Spring Data方法声明
  - [x] SubTask 1.3: 验证方法签名和返回类型正确

- [x] Task 2: 修改FileAssetController直接查询数据库获取文件记录
  - [x] SubTask 2.1: 修改FileAssetController注入StoredFileRecordRepository
  - [x] SubTask 2.2: 重构preview和download方法使用findByFileId直接查询
  - [x] SubTask 2.3: 移除对scriptProjectService.findFile的依赖
  - [x] SubTask 2.4: 确保错误处理正确（文件不存在返回404）

- [x] Task 3: 验证数据库索引配置
  - [x] SubTask 3.1: 检查StoredFileRecord实体的索引定义
  - [x] SubTask 3.2: 确认findByFileId查询有主键索引支持
  - [x] SubTask 3.3: 确认findRecent查询有created_at索引支持

- [x] Task 4: 验证修复效果
  - [x] SubTask 4.1: 启动后端服务并访问媒体资源中心
  - [x] SubTask 4.2: 点击"打开"按钮验证文件可以正常预览
  - [x] SubTask 4.3: 验证数据库中StoredFileRecord记录正确持久化

# Task Dependencies
- Task 2 depends on Task 1（需要先添加Repository方法才能使用）
- Task 3 可独立进行，与Task 1和Task 2并行
- Task 4 depends on Task 1 and Task 2
