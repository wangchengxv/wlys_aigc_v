package service

import (
	"encoding/base64"
	"encoding/json"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/example/aigc-server-go/internal/config"
	"github.com/example/aigc-server-go/internal/model"
	"github.com/google/uuid"
)

type LocalFile struct {
	DataDir string
	HTTP    *http.Client
}

func NewLocalFile(cfg *config.Config) *LocalFile {
	return &LocalFile{
		DataDir: cfg.DataDir,
		HTTP:    &http.Client{Timeout: 60 * time.Second},
	}
}

func (s *LocalFile) resolveProjectRoot(projectID string) string {
	return filepath.Join(s.DataDir, "script-projects", projectID)
}

func normalizeRelativePath(rel string) string {
	rel = strings.ReplaceAll(rel, "\\", "/")
	for strings.HasPrefix(rel, "/") {
		rel = rel[1:]
	}
	rel = strings.ReplaceAll(rel, "..", "")
	return rel
}

func (s *LocalFile) StoreText(projectID, relativePath, mediaType, content string) *model.StoredFileRecord {
	root := s.resolveProjectRoot(projectID)
	target := filepath.Join(root, filepath.FromSlash(normalizeRelativePath(relativePath)))
	_ = os.MkdirAll(filepath.Dir(target), 0o755)
	_ = os.WriteFile(target, []byte(content), 0o644)
	return s.buildRecord(projectID, target, mediaType)
}

func (s *LocalFile) StoreJSON(projectID, relativePath string, value any) *model.StoredFileRecord {
	b, _ := json.Marshal(value)
	root := s.resolveProjectRoot(projectID)
	target := filepath.Join(root, filepath.FromSlash(normalizeRelativePath(relativePath)))
	_ = os.MkdirAll(filepath.Dir(target), 0o755)
	_ = os.WriteFile(target, b, 0o644)
	return s.buildRecord(projectID, target, "application/json")
}

func (s *LocalFile) StoreBytes(projectID, relativePath, mediaType string, bytes []byte) *model.StoredFileRecord {
	root := s.resolveProjectRoot(projectID)
	target := filepath.Join(root, filepath.FromSlash(normalizeRelativePath(relativePath)))
	_ = os.MkdirAll(filepath.Dir(target), 0o755)
	_ = os.WriteFile(target, bytes, 0o644)
	return s.buildRecord(projectID, target, mediaType)
}

func (s *LocalFile) StoreRemote(projectID, relativePath, mediaType, url string) (*model.StoredFileRecord, error) {
	req, err := http.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	resp, err := s.HTTP.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 400 {
		return nil, io.ErrUnexpectedEOF
	}
	b, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	return s.StoreBytes(projectID, relativePath, mediaType, b), nil
}

func (s *LocalFile) StoreBase64(projectID, relativePath, mediaType, raw string) *model.StoredFileRecord {
	normalized := strings.TrimSpace(raw)
	if i := strings.IndexByte(normalized, ','); i >= 0 {
		normalized = normalized[i+1:]
	}
	b, err := base64.StdEncoding.DecodeString(normalized)
	if err != nil {
		b = []byte{}
	}
	return s.StoreBytes(projectID, relativePath, mediaType, b)
}

func (s *LocalFile) ReadText(rec *model.StoredFileRecord) string {
	p := s.ResolveStoredFile(rec)
	b, err := os.ReadFile(p)
	if err != nil {
		return ""
	}
	return string(b)
}

func (s *LocalFile) ReadJSON(rec *model.StoredFileRecord) map[string]any {
	var m map[string]any
	_ = json.Unmarshal([]byte(s.ReadText(rec)), &m)
	if m == nil {
		return map[string]any{}
	}
	return m
}

func (s *LocalFile) ResolveStoredFile(rec *model.StoredFileRecord) string {
	root := s.resolveProjectRoot(rec.ProjectID)
	return filepath.Join(root, filepath.FromSlash(rec.RelativePath))
}

func (s *LocalFile) ToPublicURL(fileID string) string {
	return "/api/v1/files/" + fileID
}

// ExtractProjectID matches Java LocalAssetFileService.extractProjectId (prefix before "__").
func ExtractProjectID(fileID string) string {
	if fileID == "" || !strings.Contains(fileID, "__") {
		return ""
	}
	return fileID[:strings.Index(fileID, "__")]
}

func (s *LocalFile) buildRecord(projectID, absTarget, mediaType string) *model.StoredFileRecord {
	root := s.resolveProjectRoot(projectID)
	rel, _ := filepath.Rel(root, absTarget)
	rel = filepath.ToSlash(rel)
	rec := &model.StoredFileRecord{
		ProjectID:    projectID,
		FileID:       projectID + "__" + strings.ReplaceAll(uuid.New().String(), "-", ""),
		FileName:     filepath.Base(absTarget),
		RelativePath: rel,
		MediaType:    mediaType,
		CreatedAt:    time.Now(),
	}
	if st, err := os.Stat(absTarget); err == nil {
		rec.SizeBytes = st.Size()
	}
	return rec
}
