# 视频页首帧来源与排版改造计划

## Summary
- 目标：把项目工作区视频页、独立图生视频页、`StoryboardLitePage` 的视频生成入口统一为“先选首帧来源，再生成视频”的体验。
- 范围：前后端一起改，支持 `上传图片`、`资源库点选`、`九宫格选图` 三类首帧来源；九宫格从必经步骤降级为可选来源。
- 约束：尽量复用现有 `VideoReferenceImageField`、工作台图生视频请求链路、项目页视频生成链路，减少重写。

## Current State Analysis
- 项目视频页 [ScriptProjectVideoPage.tsx](file:///Users/xingyi/Downloads/aigc/AIGC_university/aigc-site-react/src/pages/ScriptProjectVideoPage.tsx) 当前只提供“绑定九宫格首帧”，并通过 [VideoSegmentCard.tsx](file:///Users/xingyi/Downloads/aigc/AIGC_university/aigc-site-react/src/components/script/VideoSegmentCard.tsx) 暴露 `整张九宫格 / 单格裁剪 / 第几格`。
- 前端协议 [types/index.ts](file:///Users/xingyi/Downloads/aigc/AIGC_university/aigc-site-react/src/types/index.ts) 中 `ApplyStoryboardFirstFrameRequest` 仅支持 `assetId + mode + panelIndex`，模式只有 `NONE / FULL_GRID / CROPPED_PANEL`。
- 后端实现 [ScriptWorkflowService.java](file:///Users/xingyi/Downloads/aigc/AIGC_university/aigc-server/src/main/java/com/example/aigc/service/ScriptWorkflowService.java) 的 `applyStoryboardFirstFrame()` 只接受九宫格资产，并在镜头上写入 `storyboardImageFileId / storyboardCropFileId`。
- 独立图生视频工作区 [PromptPanel.tsx](file:///Users/xingyi/Downloads/aigc/AIGC_university/aigc-site-react/src/components/workspace/PromptPanel.tsx) 已支持 `videoReferenceImageUrl`，并复用了 [VideoReferenceImageField.tsx](file:///Users/xingyi/Downloads/aigc/AIGC_university/aigc-site-react/src/components/workspace/VideoReferenceImageField.tsx) 的上传、拖拽、URL/Base64 输入能力。
- `StoryboardLiteService` 已具备把远程图或 Base64 图存成项目文件的能力，可复用同类模式处理项目视频页的新首帧来源。

## Assumptions & Decisions
- 本次优先保证项目工作区视频页可直接用“上传图/资源图/九宫格图”作为首帧，独立页同步优化交互与排版。
- 项目页资源选择优先复用项目内已有资产与现有媒体记录，不额外做复杂全站素材管理器。
- 为兼容现有镜头字段，后端新增“首帧来源类型”和“直接首帧文件”支持，同时保留现有九宫格字段，避免旧数据失效。
- 拖拽交互本次先做到“拖入本地图片”和“资源卡点击/选择”；素材卡跨区域自由拖拽可作为后续增强，不阻塞本次交付。

## Proposed Changes

### 1. 后端协议与首帧绑定扩展
- 文件：
  - `aigc-server/src/main/java/com/example/aigc/dto/ApplyStoryboardFirstFrameRequest.java`
  - `aigc-server/src/main/java/com/example/aigc/dto/StoryboardFirstFrameResponse.java`
  - `aigc-server/src/main/java/com/example/aigc/service/ScriptWorkflowService.java`
- 变更：
  - 扩展请求字段，新增首帧来源类型、直接文件 ID、远程 URL/Base64 输入。
  - `applyStoryboardFirstFrame()` 支持三类来源：
    - `storyboard`: 继续支持整张九宫格与九宫格裁剪。
    - `resource`: 直接引用已有图片文件作为首帧。
    - `upload`: 接收 http(s) / data URL，落盘成项目文件后绑定。
  - 返回值补充来源类型与实际生效文件，方便前端统一渲染“当前首帧”。
- 原因：
  - 解除“必须先九宫格”的后端约束。
  - 兼容旧项目与现有视频生成链路。

### 2. 前端类型与 API 对齐
- 文件：
  - `aigc-site-react/src/types/index.ts`
  - `aigc-site-react/src/api/index.ts`
  - `aigc-site-react/src/stores/scriptProjectStore.ts`
- 变更：
  - 扩展 `ApplyStoryboardFirstFrameRequest`、镜头首帧响应/字段类型。
  - 保持 `applyStoryboardFirstFrameForShot()` 调用入口不变，但允许新来源参数透传。
  - 统一镜头首帧展示优先级：直接首帧文件 > 九宫格裁剪图 > 九宫格整图。
- 原因：
  - 保证新旧数据结构可同时工作。

### 3. 项目视频页重构为“首帧来源选择器”
- 文件：
  - `aigc-site-react/src/components/script/VideoSegmentCard.tsx`
  - `aigc-site-react/src/pages/ScriptProjectVideoPage.tsx`
- 变更：
  - 把“快捷绑定九宫格来源”重构成“选择首帧来源”模块。
  - 提供三个入口：
    - 上传图片：复用 `VideoReferenceImageField`。
    - 从项目素材中选择：优先列出可用图片资产与已有分镜图。
    - 从九宫格中选择：保留整张/单格裁剪。
  - 优化卡片排版：首帧选择区、参数区、结果区分层显示，弱化九宫格技术术语。
  - 当前已绑定首帧区域统一显示来源、预览、清除动作。
- 原因：
  - 降低用户理解成本，让“先选图再生成视频”成为主线。

### 4. 独立图生视频工作区排版优化
- 文件：
  - `aigc-site-react/src/components/workspace/PromptPanel.tsx`
  - 如有必要补充共用选择器组件到 `aigc-site-react/src/components/workspace/`
- 变更：
  - 在 `image-to-video` 入口下突出“首帧图选择”区域，把上传/拖拽放到视频参数前。
  - 增强文案，让用户明确可直接上传单图生成视频，不需要九宫格。
  - 如资源库入口易于接入，则补一个轻量“粘贴/选择已生成图片”入口；否则本次至少先把上传和 URL/Base64 流程做顺。
- 原因：
  - 统一独立工具页与项目页的认知模型。

### 5. Storyboard Lite 视频步骤优化
- 文件：
  - `aigc-site-react/src/pages/StoryboardLitePage.tsx`
  - 如有必要补充 `aigc-site-react/src/api/index.ts` 的 lite 请求类型
- 变更：
  - 第四步从“只能用已确认关键帧生成视频”调整为：
    - 默认使用已确认关键帧。
    - 可切换为上传图片或粘贴图片链接后生成。
  - 结果区保留现有视频渲染，但优化步骤说明与布局。
- 原因：
  - 让 lite 流程也和主工作区一致，不把用户锁死在关键帧链路里。

## Verification
- 前端验证：
  - 项目视频页能分别用上传图、项目内图片、九宫格图绑定首帧并显示预览。
  - 独立图生视频页上传单图后能正常提交视频任务。
  - Storyboard Lite 可在“已确认关键帧”和“自定义上传图”之间切换生成视频。
- 后端验证：
  - 旧的 `FULL_GRID / CROPPED_PANEL` 请求仍然可用。
  - 新的 `resource / upload` 请求能正确落盘或绑定文件，并回写镜头首帧。
- 工程验证：
  - 对修改文件跑语言诊断。
  - 若存在可执行的前后端测试/构建入口，至少跑与 TypeScript 编译或后端编译相关的最小验证。
