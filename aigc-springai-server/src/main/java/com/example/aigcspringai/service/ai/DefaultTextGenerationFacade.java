package com.example.aigcspringai.service.ai;

import com.example.aigcspringai.config.AiGatewayProperties;
import com.example.aigcspringai.dto.TextGenerationContext;
import com.example.aigcspringai.dto.TextGenerationRequest;
import com.example.aigcspringai.dto.TextGenerationResult;
import com.example.aigcspringai.dto.TextGenerationStreamChunk;
import com.example.aigcspringai.exception.InvalidRequestException;
import com.example.aigcspringai.exception.TextGenerationException;
import com.example.aigcspringai.service.fallback.FallbackGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DefaultTextGenerationFacade implements TextGenerationFacade {

    private final SpringAiClientFactory springAiClientFactory;
    private final FallbackGateway fallbackGateway;
    private final AiGatewayProperties aiGatewayProperties;

    public DefaultTextGenerationFacade(
            SpringAiClientFactory springAiClientFactory,
            FallbackGateway fallbackGateway,
            AiGatewayProperties aiGatewayProperties
    ) {
        this.springAiClientFactory = springAiClientFactory;
        this.fallbackGateway = fallbackGateway;
        this.aiGatewayProperties = aiGatewayProperties;
    }

    @Override
    public TextGenerationResult generate(TextGenerationRequest request, TextGenerationContext context) {
        validate(request);
        try {
            AiTextClient client = springAiClientFactory.getOrCreate(toBuildSpec(request));
            return client.generate(request);
        } catch (TextGenerationException ex) {
            if (allowFallback(ex, context)) {
                return fallbackGateway.fallbackGenerate(request, context, ex);
            }
            throw ex;
        } catch (Exception ex) {
            TextGenerationException wrapped = new TextGenerationException(
                    "SPRING_AI_CALL_FAILED",
                    502,
                    ex.getMessage() == null ? "spring ai 调用失败" : ex.getMessage(),
                    true,
                    true,
                    ex
            );
            if (allowFallback(wrapped, context)) {
                return fallbackGateway.fallbackGenerate(request, context, wrapped);
            }
            throw wrapped;
        }
    }

    @Override
    public List<TextGenerationStreamChunk> stream(TextGenerationRequest request, TextGenerationContext context) {
        validate(request);
        try {
            AiTextClient client = springAiClientFactory.getOrCreate(toBuildSpec(request));
            return client.generateStream(request);
        } catch (TextGenerationException ex) {
            if (allowFallback(ex, context)) {
                return fallbackGateway.fallbackStream(request, context, ex);
            }
            throw ex;
        } catch (Exception ex) {
            TextGenerationException wrapped = new TextGenerationException(
                    "SPRING_AI_STREAM_FAILED",
                    502,
                    ex.getMessage() == null ? "spring ai 流式调用失败" : ex.getMessage(),
                    true,
                    true,
                    ex
            );
            if (allowFallback(wrapped, context)) {
                return fallbackGateway.fallbackStream(request, context, wrapped);
            }
            throw wrapped;
        }
    }

    private SpringAiClientFactory.ClientBuildSpec toBuildSpec(TextGenerationRequest request) {
        Map<String, Object> metadata = request.extraMetadata() == null ? Map.of() : request.extraMetadata();
        return new SpringAiClientFactory.ClientBuildSpec(
                request.providerCode() + "-default",
                request.providerCode(),
                request.modelCode(),
                request.timeoutMs() == null ? 30_000L : request.timeoutMs(),
                metadata
        );
    }

    private void validate(TextGenerationRequest request) {
        if (request.providerCode() == null || request.providerCode().isBlank()) {
            throw new InvalidRequestException("providerCode 不能为空");
        }
        if (request.modelCode() == null || request.modelCode().isBlank()) {
            throw new InvalidRequestException("modelCode 不能为空");
        }
        if (request.messages() == null || request.messages().isEmpty()) {
            throw new InvalidRequestException("messages 不能为空");
        }
    }

    private boolean allowFallback(TextGenerationException ex, TextGenerationContext context) {
        boolean contextAllow = context != null && context.fallbackEnabled();
        return aiGatewayProperties.isFallbackEnabled() && contextAllow && ex.isFallbackAllowed();
    }
}
