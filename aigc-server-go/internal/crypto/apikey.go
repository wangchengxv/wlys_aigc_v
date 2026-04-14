package crypto

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"io"
	"strings"

	"github.com/example/aigc-server-go/internal/errs"
)

// Service mirrors ApiKeyCryptoService (AES/GCM/NoPadding, SHA-256 derived key).
type Service struct {
	key []byte
}

func New(encryptionKey string) (*Service, error) {
	if strings.TrimSpace(encryptionKey) == "" {
		return nil, errs.New(500, "未配置加密密钥")
	}
	sum := sha256.Sum256([]byte(encryptionKey))
	k := make([]byte, 32)
	copy(k, sum[:])
	return &Service{key: k}, nil
}

func (s *Service) Encrypt(plain string) (string, error) {
	block, err := aes.NewCipher(s.key)
	if err != nil {
		return "", errs.New(500, "API Key 加密失败")
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", errs.New(500, "API Key 加密失败")
	}
	iv := make([]byte, 12)
	if _, err := io.ReadFull(rand.Reader, iv); err != nil {
		return "", errs.New(500, "API Key 加密失败")
	}
	ct := gcm.Seal(nil, iv, []byte(plain), nil)
	out := append(iv, ct...)
	return base64.StdEncoding.EncodeToString(out), nil
}

func (s *Service) Decrypt(b64 string) (string, error) {
	payload, err := base64.StdEncoding.DecodeString(b64)
	if err != nil {
		return "", errs.New(500, "API Key 解密失败")
	}
	if len(payload) < 13 {
		return "", errs.New(500, "API Key 解密失败")
	}
	iv := payload[:12]
	ct := payload[12:]
	block, err := aes.NewCipher(s.key)
	if err != nil {
		return "", errs.New(500, "API Key 解密失败")
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", errs.New(500, "API Key 解密失败")
	}
	pt, err := gcm.Open(nil, iv, ct, nil)
	if err != nil {
		return "", errs.New(500, "API Key 解密失败")
	}
	return string(pt), nil
}

func Mask(apiKey string) string {
	if apiKey == "" {
		return ""
	}
	if len(apiKey) <= 6 {
		return strings.Repeat("*", len(apiKey))
	}
	return apiKey[:3] + strings.Repeat("*", len(apiKey)-6) + apiKey[len(apiKey)-3:]
}
