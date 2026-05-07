package com.example.aigc.controller;

import com.example.aigc.exception.BizException;
import com.example.aigc.service.ProviderGatewayException;
import com.example.aigc.service.RouterProxyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RouterProxyController {

    private final RouterProxyService routerProxyService;

    public RouterProxyController(RouterProxyService routerProxyService) {
        this.routerProxyService = routerProxyService;
    }

    @PostMapping("/v1/chat/completions")
    public ResponseEntity<?> chatCompletions(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String xApiKey
    ) {
        try {
            boolean stream = Boolean.TRUE.equals(body.get("stream"));
            if (stream) {
                StreamingResponseBody streamingResponseBody = routerProxyService.proxyChatStream(body, "openai", authorization, xApiKey);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(streamingResponseBody);
            }
            return ResponseEntity.ok(routerProxyService.proxyChat(body, "openai", authorization, xApiKey));
        } catch (BizException ex) {
            return error(ex.getStatus(), ex.getMessage());
        } catch (ProviderGatewayException ex) {
            return error(ex.getStatusCode(), ex.getMessage());
        }
    }

    @PostMapping("/v1/messages")
    public ResponseEntity<?> messages(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String xApiKey
    ) {
        try {
            boolean stream = Boolean.TRUE.equals(body.get("stream"));
            if (stream) {
                StreamingResponseBody streamingResponseBody = routerProxyService.proxyChatStream(body, "anthropic", authorization, xApiKey);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(streamingResponseBody);
            }
            return ResponseEntity.ok(routerProxyService.proxyChat(body, "anthropic", authorization, xApiKey));
        } catch (BizException ex) {
            return error(ex.getStatus(), ex.getMessage());
        } catch (ProviderGatewayException ex) {
            return error(ex.getStatusCode(), ex.getMessage());
        }
    }

    @GetMapping("/v1/models")
    public ResponseEntity<?> models(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String xApiKey
    ) {
        try {
            return ResponseEntity.ok(routerProxyService.listModels(authorization, xApiKey));
        } catch (BizException ex) {
            return error(ex.getStatus(), ex.getMessage());
        } catch (ProviderGatewayException ex) {
            return error(ex.getStatusCode(), ex.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", Map.of("message", message));
        return ResponseEntity.status(status).body(body);
    }
}
