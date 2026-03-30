请根据结构化剧本拆分出适合并发视频生成的镜头分段。

输出 JSON 数组，每项至少包含：
- id
- title
- scriptText
- actionSummary
- cameraMovement
- characterIds
- backgroundIds
- propIds

要求：
1. 每个镜头关注一个主要动作或叙事节点。
2. 保持前后镜头衔接自然，便于独立生成视频片段。
3. 只输出 JSON。
