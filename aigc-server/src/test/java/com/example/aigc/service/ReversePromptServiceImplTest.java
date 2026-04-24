package com.example.aigc.service;

import com.example.aigc.dto.ReversePromptModelOptionsData;
import com.example.aigc.dto.ReversePromptRequest;
import com.example.aigc.dto.ReversePromptResponse;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.ModelConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.ModelConfigRepository;
import com.example.aigc.service.impl.ReversePromptServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ReversePromptServiceImplTest {

    private ModelConfigRepository modelConfigRepository;
    private ConnectionConfigRepository connectionConfigRepository;
    private ModelCapabilityService modelCapabilityService;
    private ProviderCatalog providerCatalog;
    private ProviderHttpGateway providerHttpGateway;
    private ApiKeyCryptoService apiKeyCryptoService;
    private RouterRoutingService routerRoutingService;
    private ReversePromptServiceImpl service;

    @BeforeEach
    void setUp() {
        modelConfigRepository = Mockito.mock(ModelConfigRepository.class);
        connectionConfigRepository = Mockito.mock(ConnectionConfigRepository.class);
        modelCapabilityService = Mockito.mock(ModelCapabilityService.class);
        providerCatalog = Mockito.mock(ProviderCatalog.class);
        providerHttpGateway = Mockito.mock(ProviderHttpGateway.class);
        apiKeyCryptoService = Mockito.mock(ApiKeyCryptoService.class);
        routerRoutingService = Mockito.mock(RouterRoutingService.class);
        service = new ReversePromptServiceImpl(
                modelConfigRepository,
                connectionConfigRepository,
                modelCapabilityService,
                providerCatalog,
                providerHttpGateway,
                apiKeyCryptoService,
                routerRoutingService,
                new ObjectMapper()
        );
    }

    @Test
    void listModelsReturnsOnlyConfiguredDoubaoCandidates() {
        ModelConfig doubao = ModelConfig.create(
                "豆包视觉",
                "onelinkai",
                "doubao-vision-pro",
                "conn_1",
                true,
                Map.of("capabilities", List.of("text"))
        );
        ModelConfig other = ModelConfig.create(
                "其他模型",
                "onelinkai",
                "gpt-4o",
                "conn_1",
                true,
                Map.of("capabilities", List.of("text"))
        );
        ConnectionConfig connection = ConnectionConfig.create("one", "onelinkai", "https://api.onelinkai.cloud", "__enc__", true);
        ProviderCatalog.ProviderDefinition provider = new ProviderCatalog.ProviderDefinition(
                "onelinkai",
                "OneLinkAI",
                "https://api.onelinkai.cloud",
                "openai",
                "/v1/chat/completions",
                "/v1/models",
                ProviderCatalog.AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        );
        when(modelConfigRepository.findAll()).thenReturn(List.of(doubao, other));
        when(connectionConfigRepository.findById("conn_1")).thenReturn(Optional.of(connection));
        when(providerCatalog.require("onelinkai")).thenReturn(provider);
        when(modelCapabilityService.supports(ArgumentMatchers.any(ModelConfig.class), ArgumentMatchers.eq("text"))).thenAnswer(invocation -> {
            ModelConfig cfg = invocation.getArgument(0);
            return "doubao-vision-pro".equals(cfg.getModelName()) || "gpt-4o".equals(cfg.getModelName());
        });
        when(routerRoutingService.resolveOrderedConnections(true)).thenReturn(List.of(connection));

        ReversePromptModelOptionsData options = service.listModels();

        assertThat(options.options()).containsExactly("doubao-vision-pro");
        assertThat(options.defaultModel()).isEqualTo("doubao-vision-pro");
    }

    @Test
    void reversePromptRejectsInvalidImageInput() {
        assertThatThrownBy(() -> service.reversePrompt(new ReversePromptRequest("not-an-image", null), "u1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("图片仅支持");
    }

    @Test
    void reversePromptRejectsUnavailableModel() {
        ModelConfig model = ModelConfig.create(
                "豆包视觉",
                "onelinkai",
                "doubao-vision-pro",
                "conn_1",
                true,
                Map.of("capabilities", List.of("text"))
        );
        ConnectionConfig connection = ConnectionConfig.create("one", "onelinkai", "https://api.onelinkai.cloud", "__enc__", true);
        ProviderCatalog.ProviderDefinition provider = new ProviderCatalog.ProviderDefinition(
                "onelinkai",
                "OneLinkAI",
                "https://api.onelinkai.cloud",
                "openai",
                "/v1/chat/completions",
                "/v1/models",
                ProviderCatalog.AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        );
        when(modelConfigRepository.findAll()).thenReturn(List.of(model));
        when(connectionConfigRepository.findById("conn_1")).thenReturn(Optional.of(connection));
        when(providerCatalog.require("onelinkai")).thenReturn(provider);
        when(modelCapabilityService.supports(model, "text")).thenReturn(true);
        when(routerRoutingService.resolveOrderedConnections(true)).thenReturn(List.of(connection));

        assertThatThrownBy(() -> service.reversePrompt(
                new ReversePromptRequest("https://img.example/demo.png", "doubao-other"),
                "u1"
        )).isInstanceOf(BizException.class).hasMessageContaining("不在可用豆包模型");
    }

    @Test
    void reversePromptParsesStructuredResponse() {
        ModelConfig model = ModelConfig.create(
                "豆包视觉",
                "onelinkai",
                "doubao-vision-pro",
                "conn_1",
                true,
                Map.of("capabilities", List.of("text"))
        );
        ConnectionConfig connection = ConnectionConfig.create("one", "onelinkai", "https://api.onelinkai.cloud", "__enc__", true);
        ProviderCatalog.ProviderDefinition provider = new ProviderCatalog.ProviderDefinition(
                "onelinkai",
                "OneLinkAI",
                "https://api.onelinkai.cloud",
                "openai",
                "/v1/chat/completions",
                "/v1/models",
                ProviderCatalog.AuthMode.BEARER,
                true,
                null,
                null,
                null,
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        );
        when(modelConfigRepository.findAll()).thenReturn(List.of(model));
        when(connectionConfigRepository.findById("conn_1")).thenReturn(Optional.of(connection));
        when(providerCatalog.require("onelinkai")).thenReturn(provider);
        when(modelCapabilityService.supports(model, "text")).thenReturn(true);
        when(routerRoutingService.resolveOrderedConnections(true)).thenReturn(List.of(connection));
        when(routerRoutingService.timeoutSeconds()).thenReturn(30);
        when(apiKeyCryptoService.decrypt("__enc__")).thenReturn("k1");
        when(providerHttpGateway.invokeChat(
                ArgumentMatchers.eq(provider),
                ArgumentMatchers.eq("https://api.onelinkai.cloud"),
                ArgumentMatchers.eq("k1"),
                ArgumentMatchers.anyMap(),
                ArgumentMatchers.anyMap(),
                ArgumentMatchers.any()
        )).thenReturn(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of(
                                "content", """
                                        {"positivePrompt":"cinematic portrait","negativePrompt":"blurry","style":"realistic","lighting":"soft","composition":"rule of thirds","camera":"50mm","colorTone":"warm","parameters":{"aspectRatio":"3:4"}}
                                        """
                        )
                ))
        ));

        ReversePromptResponse response = service.reversePrompt(
                new ReversePromptRequest("https://img.example/demo.png", "doubao-vision-pro"),
                "u1"
        );

        assertThat(response.model()).isEqualTo("doubao-vision-pro");
        assertThat(response.positivePrompt()).isEqualTo("cinematic portrait");
        assertThat(response.negativePrompt()).isEqualTo("blurry");
        assertThat(response.parameters()).containsEntry("aspectRatio", "3:4");
    }
}
