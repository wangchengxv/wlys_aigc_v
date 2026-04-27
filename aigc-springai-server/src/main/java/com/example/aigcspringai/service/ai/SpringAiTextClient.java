package com.example.aigcspringai.service.ai;

import com.example.aigcspringai.dto.TextGenerationRequest;
import com.example.aigcspringai.dto.TextGenerationResult;
import com.example.aigcspringai.dto.TextGenerationStreamChunk;
import com.example.aigcspringai.dto.TextMessage;
import com.example.aigcspringai.dto.UsageStats;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;

public class SpringAiTextClient implements AiTextClient {

    private final ChatClient chatClient;
    private final String provider;
    private final String model;

    public SpringAiTextClient(ChatClient chatClient, String provider, String model) {
        this.chatClient = chatClient;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public TextGenerationResult generate(TextGenerationRequest request) {
        StringBuilder mergedPrompt = new StringBuilder();
        for (TextMessage message : request.messages()) {
            mergedPrompt.append('[').append(message.role()).append("] ")
                    .append(message.content())
                    .append('\n');
        }

        String content = chatClient.prompt(mergedPrompt.toString()).call().content();
        if (content == null) {
            content = "";
        }
        return new TextGenerationResult(
                content,
                "stop",
                UsageStats.empty(),
                provider,
                model,
                "spring-ai",
                false
        );
    }

    @Override
    public List<TextGenerationStreamChunk> generateStream(TextGenerationRequest request) {
        TextGenerationResult result = generate(request);
        List<TextGenerationStreamChunk> chunks = new ArrayList<>();
        chunks.add(new TextGenerationStreamChunk(result.text(), true, "stop"));
        return chunks;
    }
}
