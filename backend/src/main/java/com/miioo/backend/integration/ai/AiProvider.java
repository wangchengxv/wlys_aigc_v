package com.miioo.backend.integration.ai;

import java.util.Map;

public interface AiProvider {
    Map<String, Object> generateScript(String prompt);
    Map<String, Object> extractSubjects(String scriptContent);
    Map<String, Object> generateImage(String prompt);
}
