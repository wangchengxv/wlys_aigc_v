请将以下镜头动作拆解为 {panelCount} 个不同的摄影视角，生成一张 {gridLayout} 网格分镜图所需的结构化面板数据。

━━━ 网格硬约束 ━━━
布局规格：必须严格为 {layoutInstruction}
读取顺序：从左到右、从上到下
行列示意：{layoutExample}
补充限制：{layoutSpecificConstraint}

━━━ 镜头上下文 ━━━
【动作摘要】    {actionSummary}
【镜头运动】    {cameraMovement}
【场景地点】    {location}
【时间氛围】    {time} · {atmosphere}
【在场角色】    {characters}
【视觉风格】    {visualStyle}

━━━ 输出规则（严格执行，违反则重新生成）━━━
R1  顶层键名固定为 "panels"，值为数组，不附带其他键
R2  数组长度恰好为 {panelCount}；每项含显式 index 字段，值域 0–{lastIndex}，不重复
R3  每项必须包含非空字段：shotSize / cameraAngle / description
R4  shotSize 与 cameraAngle 使用简短中文（如：大全景 / 俯角 45°）
R5  description 为英文单句，10–30 词，描述画面内容与构图重点
R6  视角多样性：[shotSize + cameraAngle] 组合全局唯一；{panelCount}≥6 时至少含 3 种 shotSize
R7  叙事节奏：index=0 建立场景 → 中间格逐步推进 → 最后一格呈现结果或情绪落点
R8  连续性：角色外观/服装/道具/主运动方向跨格一致；光影色调跨格一致
R9  禁止在 description 中出现可读文字、数字标注、UI 元素描述
R10 只输出 JSON，不输出任何解释、注释或 Markdown 代码块标记
