# SpringAI 模型配置迁移至 aigc-server 计划

## 任务概述
将 `springai-backend` 的模型配置管理功能迁移到 `aigc-server` 项目中，使用户能够通过前端进行模型的增删改查操作。

## 迁移内容清单

### 1. 实体层（Model）
- [ ] `ConnectionConfig.java` - 连接配置实体（包含 provider、baseUrl、加密的 apiKey 等）
- [ ] `ModelConfig.java` - 模型配置实体（关联连接配置，包含 name、provider、modelName 等）

### 2. DTO 层
- [ ] `ConnectionConfigCreateRequest.java` - 创建连接请求
- [ ] `ConnectionConfigUpdateRequest.java` - 更新连接请求
- [ ] `ConnectionConfigResponse.java` - 连接响应（掩码显示 apiKey）
- [ ] `ModelConfigCreateRequest.java` - 创建模型请求
- [ ] `ModelConfigUpdateRequest.java` - 更新模型请求
- [ ] `ModelConfigResponse.java` - 模型响应

### 3. Repository 层
- [ ] `ConnectionConfigRepository.java` - 连接配置仓库接口
- [ ] `InMemoryConnectionConfigRepository.java` - 内存实现
- [ ] `ModelConfigRepository.java` - 模型配置仓库接口
- [ ] `InMemoryModelConfigRepository.java` - 内存实现

### 4. Service 层
- [ ] `ApiKeyCryptoService.java` - API Key 加密/解密服务
- [ ] `ConnectionConfigService.java` - 连接配置服务
- [ ] `ModelConfigService.java` - 模型配置服务

### 5. Controller 层
- [ ] `ConnectionConfigController.java` - 连接配置 REST API (`/api/v1/connections`)
- [ ] `ModelConfigController.java` - 模型配置 REST API (`/api/v1/models`)

### 6. 异常处理
- [ ] `BizException.java` - 业务异常
- [ ] `ErrorCode.java` - 错误码枚举
- [ ] 更新 `GlobalExceptionHandler.java` - 适配新的异常类型

### 7. 配置
- [ ] `EncryptionProperties.java` - 加密配置属性
- [ ] 更新 `application.yml` - 添加加密相关配置

## 实施步骤

### 第一步：创建包结构和实体类
1. 在 `aigc-server/src/main/java/com/example/aigc/` 下创建：
   - `model/` 目录，放入 `ConnectionConfig.java` 和 `ModelConfig.java`
2. 修改包名从 `com.openwebui.springai` 改为 `com.example.aigc`

### 第二步：创建 DTO 类
1. 在 `aigc-server/src/main/java/com/example/aigc/dto/` 下创建：
   - 6个 DTO record 类

### 第三步：创建 Repository 层
1. 在 `aigc-server/src/main/java/com/example/aigc/repository/` 下创建：
   - 接口和内存实现类

### 第四步：创建异常处理
1. 在 `aigc-server/src/main/java/com/example/aigc/exception/` 下创建：
   - `BizException.java`
   - `ErrorCode.java`
2. 更新 `GlobalExceptionHandler.java`

### 第五步：创建配置类
1. 创建 `config/EncryptionProperties.java`
2. 更新 `application.yml` 添加加密配置

### 第六步：创建 Service 层
1. 在 `aigc-server/src/main/java/com/example/aigc/service/` 下创建：
   - `ApiKeyCryptoService.java`
   - `ConnectionConfigService.java`
   - `ModelConfigService.java`

### 第七步：创建 Controller 层
1. 在 `aigc-server/src/main/java/com/example/aigc/controller/` 下创建：
   - `ConnectionConfigController.java`
   - `ModelConfigController.java`

### 第八步：验证
1. 编译项目确认无错误
2. 测试 API 端点

## 注意事项
- SpringAI 的 `ApiResponse` 与 aigc-server 的 `ApiResponse` 格式不同，需要统一
- 迁移后统一使用 aigc-server 的 `ApiResponse` 格式 `{code, message, data}`
- 所有类需要修改包名为 `com.example.aigc`
