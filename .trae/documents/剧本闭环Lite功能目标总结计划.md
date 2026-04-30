# 剧本闭环Lite 功能目标总结计划

## Summary
- 目标：明确“剧本闭环Lite”作为独立最小闭环（MVP）应实现的业务能力、边界与验收口径，避免与完整 `script-projects` 重型流程职责重叠。
- 输出：形成一份面向产品/研发共识的功能定义总结（用户路径 + 能力边界 + 成功标准），用于后续实现与迭代排期。

## Current State Analysis
- 前端已有独立页面与入口：
  - `aigc-site-react/src/pages/StoryboardLitePage.tsx` 已实现四步流程：创建会话、保存剧本并生成三视图、确认关键帧、图生视频生成与结果展示。
  - `aigc-site-react/src/router.tsx` 路由已挂载 `tools/storyboard-lite`，描述为“独立最小闭环：剧本、三视图/关键帧、图生视频”。
  - `aigc-site-react/src/components/layout/TopNav.tsx` 已在“创作工具”导航暴露“剧本闭环Lite”入口。
- 后端已有闭环 API 与服务编排：
  - `aigc-server/src/main/java/com/example/aigc/controller/StoryboardLiteController.java` 已提供会话创建、剧本保存、关键帧生成/确认、视频生成、会话查询接口。
  - `aigc-server/src/main/java/com/example/aigc/service/StoryboardLiteService.java` 已实现串联逻辑：剧本 -> 文生图关键帧 -> 关键帧确认 -> 图生视频。
  - `aigc-server/src/main/java/com/example/aigc/dto/StoryboardLiteDtos.java` 已定义输入校验与响应结构。
- 数据持久化已具备最小模型：
  - `aigc-server/src/main/resources/db/migration/V27__add_storyboard_lite_tables.sql` 已创建 session/script/keyframe/video_task 四张表。
- 现状结论：
  - “闭环骨架”已经存在，当前重点不是从 0 到 1 开发，而是明确“Lite 版本应该保证哪些必达能力、哪些明确不做、如何判断闭环成立”。

## Proposed Changes
- 文件：`aigc-site-react/src/pages/StoryboardLitePage.tsx`
  - What：保持 4 步串行交互，确保每一步都有明确前置校验与用户反馈（例如未建会话、未确认关键帧不可发起视频）。
  - Why：Lite 的核心价值是“低认知成本的一次走通”，不是功能面最全。
  - How：围绕“最短可达路径”收敛交互，优先保证一次会话可稳定产出视频结果。
- 文件：`aigc-server/src/main/java/com/example/aigc/service/StoryboardLiteService.java`
  - What：继续作为单一编排入口，严格维护状态流转：`DRAFT -> SCRIPT_READY -> KEYFRAME_READY -> VIDEO_READY`。
  - Why：清晰状态机是闭环可观测、可排障、可验收的基础。
  - How：统一异常语义、最小必需校验（会话归属、剧本存在、关键帧已确认）与结果回写。
- 文件：`aigc-server/src/main/resources/db/migration/V27__add_storyboard_lite_tables.sql`
  - What：维持“一会话多版本剧本 + 多关键帧候选 + 多视频任务记录”的可追溯数据结构。
  - Why：Lite 仍需保留最小可复盘能力，支持重试与结果追踪。
  - How：使用当前四表模型，不引入完整工程域的复杂关联。

## Assumptions & Decisions
- 业务定位（Decision）：剧本闭环Lite = **验证“从文本到可播放视频”主路径的最小产品**，服务于快速试跑、教学演示、新用户体验与模型联调。
- 必达能力（Decision）：
  - 用户可在单页内完成“剧本输入 -> 三视图生成 -> 关键帧确认 -> 图生视频生成 -> 结果查看”。
  - 全流程按会话隔离并可回查历史结果（至少当前会话可重载恢复）。
  - 失败可定位到具体步骤并返回可理解错误信息。
- 明确不做（Decision）：
  - 不承载完整剧本工程能力（资产批量抽取、分镜拆解、并发流水线、配音/口型/成片编排等重流程）。
  - 不追求复杂协作与审批，只覆盖个人闭环试跑。
- 成功标准（Decision）：
  - 端到端一次成功率可接受（可按测试样例稳定复现）。
  - 关键流程时延可控（用户感知为“可等待且有反馈”）。
  - 任一步失败不会破坏会话数据完整性，用户可继续修正后重试。

## Verification Steps
1. 功能路径验证：从 `tools/storyboard-lite` 进入，完成会话创建、剧本保存、关键帧生成与确认、视频生成，最终可在结果区播放视频。
2. 状态流转验证：检查会话状态按预期递进，不出现越级或回退异常。
3. 失败场景验证：分别验证“未建会话保存剧本”“未确认关键帧生成视频”“模型返回空结果”等错误提示可读且可恢复。
4. 数据回查验证：刷新页面后通过会话查询接口恢复 latestScript、keyframes、videoTasks。
5. 边界隔离验证：Lite 页面不依赖 `script-projects` 重型流程即可独立完成最小闭环。
