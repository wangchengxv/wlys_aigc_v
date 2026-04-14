package prompts

import (
	"embed"
	"path/filepath"
	"strings"
)

//go:embed embed
var files embed.FS

// Load reads a template path like prompts/script/refine-system.md
func Load(classpath string) ([]byte, error) {
	p := strings.TrimPrefix(classpath, "/")
	return files.ReadFile(filepath.ToSlash(filepath.Join("embed", p)))
}
