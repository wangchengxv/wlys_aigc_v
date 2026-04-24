# 修复 React 重复 key 警告计划

## 问题分析

前端报错：`Encountered two children with the same key, 'video-kling-v3'`

**根因**：[QuickModelForm.tsx:136](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-site-react/src/components/model/QuickModelForm.tsx#L136) 使用 `m.modelName` 作为 React key，但 API 返回的预设模型数据中存在两个 `modelName` 为 `video-kling-v3` 的条目：

- OneLinkAI Kling 图像生成 v2.1（provider: onelinkai）
- OneLinkAI Kling 多图参考生图 v2（provider: onelinkai）

两者 `modelName` 都是 `video-kling-v3`，导致 key 重复。

## 修复方案

在 `QuickModelForm.tsx` 中，将 modelOptions 的 key 从 `m.modelName` 改为 `${m.provider}-${m.modelName}`，确保唯一性。

## 实施步骤

1. 修改 [QuickModelForm.tsx](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-site-react/src/components/model/QuickModelForm.tsx#L136)：
   - 第 136 行：将 `key={m.modelName}` 改为 `key={`${m.provider}-${m.modelName}`}`
   - 同时确保 value 也保持唯一性，使用同样格式