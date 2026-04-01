package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.ProviderCatalogEntryDto;
import com.example.aigc.dto.ProviderCatalogListResponse;
import com.example.aigc.service.ProviderCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/provider-catalog")
public class ProviderCatalogController {

    private final ProviderCatalog providerCatalog;

    public ProviderCatalogController(ProviderCatalog providerCatalog) {
        this.providerCatalog = providerCatalog;
    }

    @GetMapping
    public ApiResponse<ProviderCatalogListResponse> list() {
        List<ProviderCatalogEntryDto> entries = providerCatalog.list().stream()
                .map(def -> new ProviderCatalogEntryDto(
                        def.key(),
                        def.displayName(),
                        def.defaultBaseUrl(),
                        def.authMode().name(),
                        def.apiFormat(),
                        def.gatewayKind().name(),
                        def.textProxySupported(),
                        def.imageGenerationPath() != null && !def.imageGenerationPath().isBlank(),
                        def.videoSubmitPath() != null && !def.videoSubmitPath().isBlank(),
                        def.staticModels()
                ))
                .toList();
        return ApiResponse.ok(new ProviderCatalogListResponse(entries));
    }

    /** Documents OAuth-only providers (e.g. Copilot) not supported in this web stack. */
    @GetMapping("/oauth-notes")
    public ApiResponse<Map<String, String>> oauthNotes() {
        return ApiResponse.ok(Map.of(
                "message",
                "GitHub Copilot、CherryIN 等依赖 Electron/OAuth 桌面流程的提供商未在本 Web 网关实现；请使用 Cherry Studio 客户端或接入兼容 OpenAI 的代理。",
                "ovms",
                "OVMS 模型下载与本地推理需 OpenVINO 运行时，本 Web 应用不提供与 Cherry 桌面相同的下载向导；请在服务器或本地自行部署模型。",
                "copilot",
                "GitHub Copilot 需 OAuth；Web 侧请改用 OpenAI 兼容中转或官方 API Key。"
        ));
    }
}
