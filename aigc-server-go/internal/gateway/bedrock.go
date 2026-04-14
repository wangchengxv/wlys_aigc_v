package gateway

import (
	"context"
	"fmt"

	"github.com/example/aigc-server-go/internal/metadata"
)

type BedrockGateway struct{}

// Converse mirrors BedrockGatewayService.converse. Full SigV4 + Converse wiring can use aws-sdk-go-v2/bedrockruntime when vendored.
func (BedrockGateway) Converse(ctx context.Context, region, accessKeyID, secretKey, sessionToken string, openAiPayload map[string]any) (map[string]any, error) {
	_ = ctx
	_ = region
	_ = accessKeyID
	_ = secretKey
	_ = sessionToken
	_ = openAiPayload
	_ = metadata.AWSRegion
	return nil, NewProviderError(502, fmt.Sprintf("Bedrock Converse 需 AWS SDK：region=%s model=%v", region, openAiPayload["model"]))
}
