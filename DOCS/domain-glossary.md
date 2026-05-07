# Domain Glossary

## 核心术语
- 项目（Project）：创作流程的顶层容器，承载剧本、主体、分镜、资产与任务。
- 剧本（Script）：故事文本与集数结构，既可 AI 生成也可上传导入。
- 主体（Subject）：角色、场景、道具三类可复用创作对象。
- 分镜（Storyboard）：镜头级创作单元，关联主体与音画描述。
- 资产（Asset）：图片、视频、音频、文档、成片等可存储可复用对象。
- AI 任务（AI Task）：异步执行记录，追踪生成与渲染状态。
- 模型配置（AI Model）：模型能力、厂商、默认参数与可用状态定义。

## 状态语义
- 任务状态：`PENDING`、`RUNNING`、`SUCCESS`、`FAILED`、`CANCELLED`。
- 分镜状态：`DRAFT`、`IMAGE_DONE`、`VIDEO_DONE`、`FINALIZED`。
- 项目状态：`NORMAL`、`ARCHIVED`。
