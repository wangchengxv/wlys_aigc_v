package server

import (
	"fmt"
	"net/http"
	"os"
	"strings"

	"github.com/example/aigc-server-go/internal/api"
	"github.com/example/aigc-server-go/internal/service"
	"github.com/gin-gonic/gin"
)

// registerFileRoutes serves script workspace files (Java FileAssetController — no auth for img src).
func registerFileRoutes(v1 *gin.RouterGroup, app *App) {
	v1.GET("/files/:fileId", func(c *gin.Context) {
		serveFile(c, app, false)
	})
	v1.GET("/files/:fileId/download", func(c *gin.Context) {
		serveFile(c, app, true)
	})
}

func serveFile(c *gin.Context, app *App, attachment bool) {
	fileID := c.Param("fileId")
	projectID := service.ExtractProjectID(fileID)
	if projectID == "" {
		c.JSON(http.StatusOK, api.Fail(404, "文件不存在"))
		return
	}
	agg, err := app.Script.Require(projectID)
	if err != nil {
		c.JSON(http.StatusOK, api.Fail(404, "文件不存在"))
		return
	}
	rec := app.Script.FindFile(agg, fileID)
	if rec == nil {
		c.JSON(http.StatusOK, api.Fail(404, "文件不存在"))
		return
	}
	path := app.Local.ResolveStoredFile(rec)
	if st, err := os.Stat(path); err != nil || st.IsDir() {
		c.JSON(http.StatusOK, api.Fail(404, "文件不存在"))
		return
	}
	mt := strings.TrimSpace(rec.MediaType)
	if mt == "" {
		mt = "application/octet-stream"
	}
	c.Header("Content-Type", mt)
	filename := strings.TrimSpace(rec.FileName)
	if filename == "" {
		filename = "file"
	}
	c.Header("Content-Disposition", contentDispositionHeader(attachment, filename))
	c.File(path)
}

func contentDispositionHeader(attachment bool, filename string) string {
	fn := strings.ReplaceAll(strings.TrimSpace(filename), `"`, `'`)
	if fn == "" {
		fn = "file"
	}
	kind := "inline"
	if attachment {
		kind = "attachment"
	}
	return fmt.Sprintf(`%s; filename="%s"`, kind, fn)
}
