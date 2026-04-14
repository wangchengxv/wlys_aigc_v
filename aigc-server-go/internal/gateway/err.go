package gateway

// ProviderError mirrors ProviderGatewayException
type ProviderError struct {
	StatusCode int
	Message    string
}

func (e *ProviderError) Error() string { return e.Message }

func NewProviderError(code int, msg string) *ProviderError {
	return &ProviderError{StatusCode: code, Message: msg}
}
