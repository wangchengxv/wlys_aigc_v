项目信息：
- 项目名：{{projectName}}
- 风格：{{visualStyle}}
- 比例：{{aspectRatio}}
- 时长：{{targetDuration}} 秒
- 语言：{{language}}

请将下面原始剧本整理为适合视频生成流水线消费的 JSON。
特别注意：
- 必须保留原文中的核心人物、幕次、场景和动作链路；
- `characters` 只能填具体角色名，禁止“风格/场景/同人剧本/时长”等非角色词；
- `segments[].scriptText` 必须贴近原文内容，不要生成通用模板文案。

{{originalScript}}
