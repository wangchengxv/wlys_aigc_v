# ARCHITECTURE - 系统地图

## 1. 一图总览
```text
用户浏览器
  -> React 前端应用（工作台）
  -> API 网关 / Nginx
  -> Spring Boot 后端
      -> MySQL（业务数据）
      -> Redis（缓存与任务进度）
      -> MinIO/OSS（资产存储）
      -> 任务执行器（异步）
          -> Spring AI + 模型适配层
              -> LLM / 图片模型 / 视频模型 / 音频模型
```

## 2. 核心业务主线
```text
项目创建
  -> 剧本生成/上传
  -> 主体提取与生成（角色/场景/道具）
  -> 分镜初始化与编辑
  -> 分镜图/分镜视频生成
  -> 时间线剪辑与渲染导出
  -> 资产入库与复用
```

## 3. 前端边界
- 技术：React 18 + TypeScript + Ant Design + TanStack Query + Zustand。
- 页面域：`Home`、`ProjectList`、`ProjectWorkspace`、`AssetLibrary`、`Tasks`、`Models`。
- 工作台子域：`GlobalSettings`、`Script`、`Subject`、`Storyboard`、`Render`、`Asset`、`Task`。
- 原则：页面层只做交互编排；请求逻辑集中到 `api/`；状态按全局与页面职责拆分。

## 4. 后端边界
- 技术：Spring Boot 3.x、Spring Security + JWT、MyBatis Plus、Redis、Spring AI。
- 分层：
  - `controller`：参数接收、鉴权入口、响应封装。
  - `service`：业务编排、事务、幂等控制。
  - `mapper/entity`：持久化读写。
  - `integration`：模型、存储、通知外部适配。
- 原则：AI 调用必须通过统一能力层，不直接散落在业务模块中。

## 5. 模块地图（领域 -> 包）
- 认证：`auth`
- 项目：`project`
- 剧本：`script`
- 主体：`subject`
- 分镜：`storyboard`
- 导出：`render`
- 资产：`asset`
- 任务：`aitask`
- 模型配置：`model`
- 通知：`notification`

## 6. 数据模型主干
- 核心实体：`user`、`project`、`script`、`script_episode`、`subject`、`storyboard`、`asset`、`ai_task`、`ai_model`。
- 关键关系：
  - `project` 归属 `user`。
  - `script`、`subject`、`storyboard`、`asset` 均挂靠 `project`。
  - `subject_storyboard_rel` 建立主体与镜头引用关系。
  - `ai_task` 追踪所有长耗时生成与渲染任务。

## 7. 异步任务地图
```text
创建任务请求
  -> 参数与权限校验
  -> 写 ai_task(PENDING)
  -> 投递执行器/MQ
  -> 立即返回 taskId
  -> 后台执行并更新 progress/status
  -> 成功写业务表 + asset
  -> 失败记录 error_message
```

## 8. 横切能力
- 鉴权：所有业务接口默认登录态，项目级操作必须校验归属。
- 存储：对象存储统一接入，业务不直接依赖本地磁盘。
- 可观测：任务状态、错误信息、审计日志必须可追踪。
- 可扩展：模型厂商通过适配器扩展，避免改动核心业务流程。
