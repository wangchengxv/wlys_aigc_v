package com.example.aigc.service;

/**
 * Routing strategy for {@link ProviderCatalog.ProviderDefinition}; drives UI panels and {@link ProviderHttpGateway}.
 */
public enum GatewayKind {
    /** OpenAI-compatible JSON over HTTP (Bearer or none). */
    OPENAI_COMPAT,
    /** Anthropic Messages API. */
    ANTHROPIC,
    /** Azure OpenAI (api-key header, deployment-scoped paths). */
    AZURE_OPENAI,
    /** AWS Bedrock Runtime (SigV4). */
    BEDROCK,
    /** Google Vertex AI (OAuth2 / service account). */
    VERTEX,
    /** Local Ollama (tags API for models). */
    OLLAMA
}
