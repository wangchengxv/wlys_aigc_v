package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.ReversePromptModelOptionsData;
import com.example.aigc.dto.ReversePromptRequest;
import com.example.aigc.dto.ReversePromptResponse;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.ReversePromptService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reverse-prompt")
public class ReversePromptController {

    private final ReversePromptService reversePromptService;
    private final RequestAuthService requestAuthService;

    public ReversePromptController(
            ReversePromptService reversePromptService,
            RequestAuthService requestAuthService
    ) {
        this.reversePromptService = reversePromptService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping("/models")
    public ApiResponse<ReversePromptModelOptionsData> models(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(reversePromptService.listModels());
    }

    @PostMapping("/generate")
    public ApiResponse<ReversePromptResponse> generate(
            @Valid @RequestBody ReversePromptRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId
    ) {
        String ownerId = requestAuthService.requireUserId(authorization, xAigcToken, xUserId);
        return ApiResponse.ok(reversePromptService.reversePrompt(request, ownerId));
    }
}
