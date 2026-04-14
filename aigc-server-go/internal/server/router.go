package server

import (
	"net/http"
	"strconv"
	"strings"

	"github.com/example/aigc-server-go/internal/api"
	"github.com/example/aigc-server-go/internal/errs"
	"github.com/example/aigc-server-go/internal/middleware"
	"github.com/gin-gonic/gin"
)

// Register mounts all HTTP routes (parity with Spring controllers).
func Register(r *gin.Engine, app *App) {
	cfg := app.Config
	RegisterComfy(r, cfg.ComfyBaseURL)

	r.NoRoute(func(c *gin.Context) {
		c.JSON(http.StatusNotFound, api.Fail(404, "not found"))
	})

	apiGroup := r.Group("/api")
	apiGroup.Use(middleware.CORSAPI(cfg))

	v1 := apiGroup.Group("/v1")
	v1.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, api.OK(map[string]any{
			"ok": true, "service": "aigc-server", "version": "v1",
		}))
	})

	registerFileRoutes(v1, app)

	authed := v1.Group("")
	authed.Use(middleware.RequireAPIToken(cfg))
	authed.Use(middleware.RequireUserID(cfg))

	authed.POST("/generate", func(c *gin.Context) {
		var req map[string]any
		if err := c.BindJSON(&req); err != nil {
			c.JSON(http.StatusOK, api.Fail(400, "请求参数格式错误"))
			return
		}
		uid := c.GetString(middleware.CtxUserID)
		out, err := app.Gen.Generate(uid, req)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(out))
	})

	authed.GET("/history", func(c *gin.Context) {
		page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
		ps, _ := strconv.Atoi(c.DefaultQuery("pageSize", "20"))
		modePtr, err := parseHistoryMode(c.Query("mode"))
		if err != nil {
			writeErr(c, err)
			return
		}
		uid := c.GetString(middleware.CtxUserID)
		out, err := app.Gen.History(page, ps, modePtr, uid)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(out))
	})

	authed.GET("/tasks/:taskId", func(c *gin.Context) {
		uid := c.GetString(middleware.CtxUserID)
		out, err := app.Gen.TaskDetail(c.Param("taskId"), uid)
		if err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OK(out))
	})

	authed.DELETE("/tasks/:taskId", func(c *gin.Context) {
		uid := c.GetString(middleware.CtxUserID)
		if err := app.Gen.DeleteTask(c.Param("taskId"), uid); err != nil {
			writeErr(c, err)
			return
		}
		c.JSON(http.StatusOK, api.OKNil())
	})

	authed.GET("/models/image", func(c *gin.Context) {
		c.JSON(http.StatusOK, api.OK(app.Gen.ImageModelOptions()))
	})

	authed.GET("/models/video", func(c *gin.Context) {
		c.JSON(http.StatusOK, api.OK(app.Gen.VideoModelOptions()))
	})

	registerCRUD(authed, app)
}

func writeErr(c *gin.Context, err error) {
	if biz, ok := err.(*errs.Biz); ok {
		c.JSON(http.StatusOK, api.Fail(biz.Status, biz.Message))
		return
	}
	c.JSON(http.StatusOK, api.Fail(500, "服务异常，请稍后重试"))
}

func parseHistoryMode(raw string) (*string, error) {
	s := strings.TrimSpace(raw)
	if s == "" || strings.EqualFold(s, "all") {
		return nil, nil
	}
	switch strings.ToLower(s) {
	case "text", "image", "video", "both":
		v := strings.ToLower(s)
		return &v, nil
	default:
		return nil, errs.New(400, "mode仅支持text/image/both/video/all")
	}
}
