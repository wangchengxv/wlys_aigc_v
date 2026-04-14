package middleware

import (
	"log"
	"net/http"

	"github.com/example/aigc-server-go/internal/api"
	"github.com/example/aigc-server-go/internal/errs"
	"github.com/gin-gonic/gin"
)

func Recovery() gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if r := recover(); r != nil {
				log.Printf("panic: %v", r)
				c.JSON(http.StatusOK, api.Fail(500, "服务异常，请稍后重试"))
				c.Abort()
			}
		}()
		c.Next()
	}
}

// ErrorHandler maps Biz errors to ApiResponse (same as GlobalExceptionHandler).
func ErrorHandler() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Next()
		if len(c.Errors) == 0 {
			return
		}
		e := c.Errors.Last().Err
		if biz, ok := e.(*errs.Biz); ok {
			c.JSON(http.StatusOK, api.Fail(biz.Status, biz.Message))
			return
		}
		c.JSON(http.StatusOK, api.Fail(500, "服务异常，请稍后重试"))
	}
}
