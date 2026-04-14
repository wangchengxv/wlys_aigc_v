package com.example.aigc.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PromptVersionSource {
    @JsonProperty("ai-generated")
    AI_GENERATED,
    @JsonProperty("manual-edit")
    MANUAL_EDIT,
    @JsonProperty("rollback")
    ROLLBACK,
    @JsonProperty("imported")
    IMPORTED,
    @JsonProperty("system")
    SYSTEM
}
