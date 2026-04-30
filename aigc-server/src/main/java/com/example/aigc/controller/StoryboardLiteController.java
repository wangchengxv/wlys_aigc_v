package com.example.aigc.controller;

import com.example.aigc.config.RequestContextAttributes;
import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.StoryboardLiteDtos;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.StoryboardLiteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storyboard-lite")
public class StoryboardLiteController {
    private final StoryboardLiteService storyboardLiteService;

    public StoryboardLiteController(StoryboardLiteService storyboardLiteService) {
        this.storyboardLiteService = storyboardLiteService;
    }

    @PostMapping("/sessions")
    public ApiResponse<StoryboardLiteDtos.SessionData> createSession(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @Valid @RequestBody StoryboardLiteDtos.CreateSessionRequest request
    ) {
        return ApiResponse.ok(storyboardLiteService.createSession(userContext.userId(), request));
    }

    @PutMapping("/sessions/{sessionId}/script")
    public ApiResponse<StoryboardLiteDtos.SessionData> saveScript(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String sessionId,
            @Valid @RequestBody StoryboardLiteDtos.SaveScriptRequest request
    ) {
        return ApiResponse.ok(storyboardLiteService.saveScript(userContext.userId(), sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/keyframes/generate")
    public ApiResponse<List<StoryboardLiteDtos.KeyframeData>> generateKeyframes(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String sessionId,
            @Valid @RequestBody StoryboardLiteDtos.GenerateKeyframesRequest request
    ) {
        return ApiResponse.ok(storyboardLiteService.generateKeyframes(userContext.userId(), sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/keyframes/{keyframeId}/confirm")
    public ApiResponse<StoryboardLiteDtos.ConfirmKeyframeResponse> confirmKeyframe(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String sessionId,
            @PathVariable String keyframeId
    ) {
        return ApiResponse.ok(storyboardLiteService.confirmKeyframe(userContext.userId(), sessionId, keyframeId));
    }

    @PostMapping("/sessions/{sessionId}/video/generate")
    public ApiResponse<StoryboardLiteDtos.VideoTaskData> generateVideo(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String sessionId,
            @Valid @RequestBody StoryboardLiteDtos.GenerateVideoRequest request
    ) {
        return ApiResponse.ok(storyboardLiteService.generateVideo(userContext.userId(), sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<StoryboardLiteDtos.SessionData> querySession(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String sessionId
    ) {
        return ApiResponse.ok(storyboardLiteService.getSession(userContext.userId(), sessionId));
    }
}
