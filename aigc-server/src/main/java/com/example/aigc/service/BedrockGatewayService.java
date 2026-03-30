package com.example.aigc.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsRequest;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BedrockGatewayService {

    public BedrockGatewayService() {
    }

    public List<String> listFoundationModelIds(String region, String accessKeyId, String secretAccessKey, String sessionToken) {
        try (BedrockClient client = BedrockClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKeyId, secretAccessKey, sessionToken))
                .build()) {
            ListFoundationModelsResponse resp = client.listFoundationModels(ListFoundationModelsRequest.builder().build());
            List<String> out = new ArrayList<>();
            for (FoundationModelSummary s : resp.modelSummaries()) {
                if (s.modelId() != null) {
                    out.add(s.modelId());
                }
            }
            return out;
        }
    }

    public Map<String, Object> converse(
            String region,
            String accessKeyId,
            String secretAccessKey,
            String sessionToken,
            Map<String, Object> openAiPayload
    ) {
        Object modelObj = openAiPayload.get("model");
        String modelId = modelObj == null ? "" : String.valueOf(modelObj).trim();
        if (modelId.isEmpty()) {
            throw new ProviderGatewayException(400, "缺少 model");
        }
        List<Message> messages = buildMessages(openAiPayload);
        if (messages.isEmpty()) {
            throw new ProviderGatewayException(400, "缺少 messages");
        }
        Double temperature = doubleVal(openAiPayload.get("temperature"), 0.8);
        Integer maxTokens = intVal(openAiPayload.get("max_tokens"), 1024);

        try (BedrockRuntimeClient runtime = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKeyId, secretAccessKey, sessionToken))
                .build()) {
            ConverseRequest.Builder req = ConverseRequest.builder()
                    .modelId(modelId)
                    .messages(messages)
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(maxTokens)
                            .temperature(temperature.floatValue())
                            .build());
            ConverseResponse resp = runtime.converse(req.build());
            String text = extractOutputText(resp);
            return openAiShaped(text);
        } catch (ProviderGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderGatewayException(502, "Bedrock 调用失败: " + ex.getMessage());
        }
    }

    private static Double doubleVal(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return o == null ? def : Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private static Integer intVal(Object o, int def) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return o == null ? def : Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private List<Message> buildMessages(Map<String, Object> openAiPayload) {
        Object raw = openAiPayload.get("messages");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Message> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            String role = String.valueOf(m.get("role")).toLowerCase();
            ConversationRole br = switch (role) {
                case "user" -> ConversationRole.USER;
                case "assistant" -> ConversationRole.ASSISTANT;
                default -> ConversationRole.USER;
            };
            String content = extractTextContent(m.get("content"));
            if (content.isBlank()) {
                continue;
            }
            out.add(Message.builder()
                    .role(br)
                    .content(ContentBlock.fromText(content))
                    .build());
        }
        return out;
    }

    private String extractTextContent(Object contentNode) {
        if (contentNode == null) {
            return "";
        }
        if (contentNode instanceof String s) {
            return s;
        }
        if (contentNode instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object p : parts) {
                if (p instanceof Map<?, ?> pm) {
                    Object t = pm.get("text");
                    if (t != null) {
                        sb.append(t);
                    }
                }
            }
            return sb.toString();
        }
        return String.valueOf(contentNode);
    }

    private String extractOutputText(ConverseResponse resp) {
        if (resp.output() == null || resp.output().message() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : resp.output().message().content()) {
            if (block.text() != null) {
                sb.append(block.text());
            }
        }
        return sb.toString();
    }

    private Map<String, Object> openAiShaped(String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", text);
        Map<String, Object> choice = new HashMap<>();
        choice.put("message", message);
        choice.put("index", 0);
        return Map.of("choices", List.of(choice));
    }

    private StaticCredentialsProvider credentials(String accessKeyId, String secretAccessKey, String sessionToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken));
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }
}
