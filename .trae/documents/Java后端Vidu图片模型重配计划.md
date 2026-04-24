# Java后端 Vidu 图片模型重配计划

## Summary
- 目标：在 `aigc-server` 中把 Vidu 图片能力按 `reference2image` 规格重配，确保 `vidu`、`vidu_onelink`、`onelinkai` 三条链路都可用于图片生成。
- 成功标准：
  - Vidu 相关模型在后端可同时参与图片与视频路由（双能力）。
  - `viduq2` 支持 0-7 张参考图（可文生图）；`viduq1` 支持 1-7 张参考图。
  - 参考图校验与官方约束对齐（格式/尺寸/宽高比/大小）。
  - 现有 Vidu 视频链路（`img2video`）不回归。

## Current State Analysis
- 代码已存在 `reference2image` 调用链路：`GenerationServiceImpl` 内部已实现 `generateImagesWithViduReference2Image` 与 `callViduReference2ImageApi`。
- 当前主要缺口：
  - `PresetModelRegistry` 中 Vidu 模型能力几乎全是 `video`，导致图片模型选项与自动路由不一致。
  - `ModelCapabilityService` 对 Vidu 识别偏视频，且未覆盖 `image-vidu-*` 前缀的一致识别。
  - `generateImagesWithViduReference2Image` 当前硬性要求至少 1 张图，不符合 `viduq2` 可 0 图文生图规则。
  - `validateViduImageRef` 约束仍是 JPEG/PNG + 10MB + 0.4~2.5，不符合本次要求的严格官方口径。
- 路由现状：
  - `ProviderCatalog` 中 `vidu`/`vidu_onelink` 已有视频提交与查询路径；
  - 图片高级能力通过 `GenerationServiceImpl.resolveViduImageProvider` 在 `onelinkai -> vidu_onelink` 做了映射，具备扩展基础。
- 测试现状：
  - 已有 `GenerationServiceImplViduTest` 与 `ModelCapabilityServiceViduTest`，但断言仍以“Vidu=视频优先”为主，需要同步更新并补充新场景。

## Proposed Changes

### 1) 预置模型能力重配
- 文件：`aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
- 变更：
  - 将 Vidu 相关模型按“可图片+可视频”重配能力（至少覆盖 `viduq1`、`viduq2` 及相关别名形态）。
  - 保持非图片能力模型（如纯文本）不变。
- 原因：
  - Quick Create 与预置模型导入依赖这里的 `capabilities`，不改会导致前后端模型可见性不一致。

### 2) 能力推断与元数据标准化对齐
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ModelCapabilityService.java`
- 变更：
  - 扩展 Vidu 识别规则，显式兼容 `image-vidu-` 前缀（与 `viduq*`、`vidu*` 同级）。
  - 调整 Vidu 默认能力推断：对 Vidu Q1/Q2 族补充 `image` 能力，同时保留 `video`。
  - 保持现有 metadata 显式配置优先，不覆盖用户手工配置。
- 原因：
  - 手工导入模型或无 preset 的场景依赖推断逻辑，需保证能力一致与可解释。

### 3) reference2image 业务逻辑与参数规则修正
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 变更：
  - 放宽 `generateImagesWithViduReference2Image`：
    - `viduq2`：允许 `images` 为空（文生图）；
    - `viduq1`：仍要求至少 1 张图；
    - 两者都限制最大 7 张。
  - 调整 Vidu 参考图校验：
    - 支持 `png/jpeg/jpg/webp`；
    - 单图大小上限改为 50MB；
    - 像素下限至少 `128x128`；
    - 宽高比限制为 `1:4` 到 `4:1` 范围内；
    - 继续支持 URL 与 `data:image/...;base64,...`。
  - 更新错误文案，准确反映新约束与模型差异（`viduq1` vs `viduq2`）。
- 原因：
  - 当前逻辑与规格存在功能差异，直接影响请求可用性与用户体验。

### 4) Provider 路径与能力可调用性核对
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`（仅在需要时最小调整）
- 变更策略：
  - 维持当前 `reference2image` 由 `GenerationServiceImpl.callViduReference2ImageApi` 控制路径（`/ent/v2/reference2image` 与 `/vidu/ent/v2/reference2image`）；
  - 若检查到图片能力判定依赖 `imageGenerationPath` 导致 Vidu 在其它入口不可调用，再补充最小路径配置并同步测试。
- 原因：
  - 避免过度改动 provider 全局行为，同时满足三 provider 覆盖目标。

### 5) 测试更新与补充
- 文件：
  - `aigc-server/src/test/java/com/example/aigc/service/ModelCapabilityServiceViduTest.java`
  - `aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
- 变更：
  - 更新 Vidu 能力断言为“双能力”。
  - 新增/调整用例覆盖：
    - `viduq2` 无图文生图可通过；
    - `viduq1` 无图被拒绝；
    - `>7` 张被拒绝；
    - `webp`/50MB/尺寸/宽高比规则分支。
- 原因：
  - 锁定本次重配行为，降低后续回归风险。

## Assumptions & Decisions
- 已确认决策：
  - 采用 Vidu 双能力策略（image + video）。
  - 覆盖 `vidu`、`vidu_onelink`、`onelinkai` 三 provider。
  - `reference2image` 校验按官方规格严格对齐。
- 实施边界：
  - 本次不新增独立 Controller API，沿用现有 `GenerateRequest.advancedMedia.image.extra.reference2image` 通道。
  - 不改动与 Vidu 无关的其它 provider 行为。

## Verification Steps
- 单元测试：
  - 运行 `GenerationServiceImplViduTest`、`ModelCapabilityServiceViduTest`，确保全部通过。
- 回归检查：
  - 验证图片模型列表可出现重配后的 Vidu 模型（具备 `image` 能力）。
  - 验证 `advancedMedia.image.extra.reference2image`：
    - `viduq2 + 0图 + prompt` 成功发起；
    - `viduq1 + 0图` 返回 400；
    - `images` 超 7 返回 400；
    - 非法图片格式/尺寸/比例/体积返回预期错误。
  - 验证视频路径不回归：
    - Vidu `img2video` 与查询流程仍可正常提交与轮询。
- 代码质量：
  - 对改动文件执行诊断，确保无新增编译或静态检查错误。
