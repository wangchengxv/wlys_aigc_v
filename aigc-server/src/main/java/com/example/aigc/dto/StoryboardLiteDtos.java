package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class StoryboardLiteDtos {
    private StoryboardLiteDtos() {
    }

    public record CreateSessionRequest(
            @Size(max = 64, message = "projectId过长") String projectId,
            @Size(max = 255, message = "title过长") String title
    ) {
    }

    public record SaveScriptRequest(
            @NotBlank(message = "scriptText不能为空")
            @Size(max = 50000, message = "scriptText过长")
            String scriptText
    ) {
    }

    public record GenerateKeyframesRequest(
            @Size(max = 120, message = "imageModel过长") String imageModel,
            @Size(max = 500, message = "style过长") String style,
            @Size(max = 2000, message = "prompt过长") String prompt
    ) {
    }

    public record ConfirmKeyframeResponse(String keyframeId, boolean selected) {
    }

    public record GenerateVideoRequest(
            @Size(max = 64, message = "keyframeId过长")
            String keyframeId,
            @Size(max = 120, message = "videoModel过长")
            String videoModel,
            @Size(max = 500, message = "style过长")
            String style,
            @Size(max = 500, message = "prompt过长")
            String prompt,
            @Size(max = 10_000_000, message = "referenceImageUrl过长")
            String referenceImageUrl
    ) {
    }

    public record SessionData(
            String sessionId,
            String ownerId,
            String projectId,
            String title,
            String status,
            String latestScript,
            List<KeyframeData> keyframes,
            List<VideoTaskData> videoTasks
    ) {
    }

    public record KeyframeData(
            String keyframeId,
            String promptText,
            String imageUrl,
            String imageFileId,
            String modelName,
            boolean selected,
            String status,
            String createdAt
    ) {
    }

    public record VideoTaskData(
            String videoTaskId,
            String keyframeId,
            String status,
            String videoUrl,
            String resultVideoFileId,
            String modelName,
            String errorMessage,
            String createdAt
    ) {
    }
}
