package com.example.aigc.controller;

import com.example.aigc.config.AigcComfyProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/api/comfy")
public class ComfyProxyController {

    private static final String PREFIX = "/api/comfy";

    private final AigcComfyProperties comfyProperties;
    private final HttpClient httpClient;

    public ComfyProxyController(AigcComfyProperties comfyProperties) {
        this.comfyProperties = comfyProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @RequestMapping(value = {"", "/", "/**"})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] requestBody) {
        try {
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            String targetUrl = buildTargetUrl(request);
            HttpRequest.BodyPublisher body = requestBody == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(requestBody);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(120))
                    .method(method.name(), body);
            String contentType = request.getContentType();
            if (contentType != null && !contentType.isBlank()) {
                builder.header(HttpHeaders.CONTENT_TYPE, contentType);
            }
            HttpResponse<byte[]> proxied = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

            HttpHeaders headers = new HttpHeaders();
            String proxiedContentType = proxied.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(MediaType.APPLICATION_JSON_VALUE);
            headers.set(HttpHeaders.CONTENT_TYPE, proxiedContentType);
            return new ResponseEntity<>(proxied.body(), headers, HttpStatusCode.valueOf(proxied.statusCode()));
        } catch (Exception ex) {
            byte[] body = ("{\"error\":\"Comfy 代理请求失败: " + ex.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(body, headers, HttpStatusCode.valueOf(502));
        }
    }

    private String buildTargetUrl(HttpServletRequest request) {
        String base = trimTrailingSlash(comfyProperties.getBaseUrl());
        String uri = request.getRequestURI();
        String path = uri.startsWith(PREFIX) ? uri.substring(PREFIX.length()) : "";
        if (path.isBlank()) {
            path = "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String query = request.getQueryString();
        return query == null || query.isBlank() ? base + path : base + path + "?" + query;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8188";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
