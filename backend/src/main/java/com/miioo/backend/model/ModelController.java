package com.miioo.backend.model;

import com.miioo.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
public class ModelController {
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(List.of(
                Map.of("id", 1, "provider", "mock", "modelCode", "mock-llm", "capability", "SCRIPT"),
                Map.of("id", 2, "provider", "mock", "modelCode", "mock-image", "capability", "IMAGE")
        ));
    }
}
