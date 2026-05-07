package com.miioo.backend.integration.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MockAiProvider implements AiProvider {
    @Override
    public Map<String, Object> generateScript(String prompt) {
        return Map.of("title", "Mock Script", "content", "根据输入生成的模拟剧本: " + prompt);
    }

    @Override
    public Map<String, Object> extractSubjects(String scriptContent) {
        return Map.of(
                "characters", List.of("主角A", "配角B"),
                "scenes", List.of("教室", "街道"),
                "props", List.of("相机", "书包")
        );
    }

    @Override
    public Map<String, Object> generateImage(String prompt) {
        return Map.of(
                "url", "https://example.com/mock-image.png",
                "prompt", prompt
        );
    }
}
