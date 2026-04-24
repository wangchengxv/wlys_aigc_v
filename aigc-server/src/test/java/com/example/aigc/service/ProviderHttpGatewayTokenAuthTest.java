package com.example.aigc.service;

import com.example.aigc.service.ProviderCatalog.AuthMode;
import com.example.aigc.service.ProviderCatalog.ProviderDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderHttpGatewayTokenAuthTest {

    @Test
    void applyHeadersUsesTokenAuthorizationForVidu() throws Exception {
        ProviderHttpGateway gateway = new ProviderHttpGateway(
                new ObjectMapper(),
                Mockito.mock(BedrockGatewayService.class),
                Mockito.mock(VertexAiGatewayService.class)
        );
        ProviderDefinition vidu = new ProviderDefinition(
                "vidu",
                "Vidu",
                "https://api.onelinkai.cloud",
                "vidu",
                null,
                null,
                AuthMode.TOKEN,
                false,
                null,
                "/vidu/ent/v2/img2video",
                "/ent/v2/tasks/{taskId}/creations",
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        );
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://api.onelinkai.cloud/vidu/ent/v2/img2video"));

        Method applyHeaders = ProviderHttpGateway.class.getDeclaredMethod(
                "applyHeaders",
                HttpRequest.Builder.class,
                ProviderDefinition.class,
                String.class,
                Map.class
        );
        applyHeaders.setAccessible(true);
        applyHeaders.invoke(gateway, builder, vidu, "secret-key", Map.of());

        HttpRequest request = builder.GET().build();
        assertThat(request.headers().firstValue("Authorization")).hasValue("Token secret-key");
    }

    @Test
    void applyHeadersUsesBearerAuthorizationForViduOnelink() throws Exception {
        ProviderHttpGateway gateway = new ProviderHttpGateway(
                new ObjectMapper(),
                Mockito.mock(BedrockGatewayService.class),
                Mockito.mock(VertexAiGatewayService.class)
        );
        ProviderDefinition viduOnelink = new ProviderDefinition(
                "vidu_onelink",
                "Vidu (OneLink)",
                "https://api.onelinkai.cloud",
                "openai",
                null,
                null,
                AuthMode.BEARER,
                false,
                null,
                "/vidu/ent/v2/img2video",
                "/vidu/ent/v2/tasks/{taskId}/creations",
                GatewayKind.OPENAI_COMPAT,
                List.of(),
                null,
                false
        );
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://api.onelinkai.cloud/vidu/ent/v2/reference2image"));

        Method applyHeaders = ProviderHttpGateway.class.getDeclaredMethod(
                "applyHeaders",
                HttpRequest.Builder.class,
                ProviderDefinition.class,
                String.class,
                Map.class
        );
        applyHeaders.setAccessible(true);
        applyHeaders.invoke(gateway, builder, viduOnelink, "secret-key", Map.of());

        HttpRequest request = builder.GET().build();
        assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer secret-key");
    }
}
