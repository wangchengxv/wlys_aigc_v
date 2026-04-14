package com.example.aigc.dto;

import java.util.List;
import java.util.Map;

public record UpdateAssetRequest(
        String name,

        String description,

        List<String> tags,

        String promptDraft,

        Map<String, Object> metadata,

        /** 手动编辑视觉提示词时传入；服务端写入版本历史 */
        String visualPrompt
) {
}
