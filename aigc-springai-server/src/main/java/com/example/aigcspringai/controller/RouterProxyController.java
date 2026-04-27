package com.example.aigcspringai.controller;

import com.example.aigcspringai.dto.TextGenerationStreamChunk;
import com.example.aigcspringai.exception.TextGenerationException;
import com.example.aigcspringai.service.RouterProxyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RouterProxyController {

    private final RouterProxyService routerProxyService;

    public RouterProxyController(RouterProxyService routerProxyService) {
        this.routerProxyService = routerProxyService;
    }

    @PostMapping("/v1/chat/completions")
    public ResponseEntity<?> chatCompletions(@RequestBody Map<String, Object> body) {
        try {
            boolean stream = Boolean.TRUE.equals(body.get("stream"));
            if (!stream) {
                return ResponseEntity.ok(routerProxyService.proxyChat(body, "openai"));
            }
            List<TextGenerationStreamChunk> chunks = routerProxyService.proxyChatStream(body, "openai");
            StreamingResponseBody sse = out -> {
                for (TextGenerationStreamChunk chunk : chunks) {
                    String payload = "data: " + chunk.delta() + "\n\n";
                    out.write(payload.getBytes(StandardCharsets.UTF_8));
                }
                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            };
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(sse);
        } catch (TextGenerationException ex) {
            return error(ex.getHttpStatus(), ex.getMessage(), ex.getErrorCode());
        }
    }

    @PostMapping("/v1/messages")
    public ResponseEntity<?> messages(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(routerProxyService.proxyChat(body, "anthropic"));
        } catch (TextGenerationException ex) {
            return error(ex.getHttpStatus(), ex.getMessage(), ex.getErrorCode());
        }
    }

    @GetMapping("/v1/models")
    public Map<String, Object> models() {
        return Map.of(
                "object", "list",
                "data", List.of(
                        Map.of("id", "gpt-4o-mini", "object", "model", "owned_by", "openai"),
                        Map.of("id", "claude-3-5-sonnet", "object", "model", "owned_by", "anthropic")
                )
        );
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", Map.of("message", message, "code", code));
        return ResponseEntity.status(status).body(body);
    }
}
