package errs

import "fmt"

// Biz mirrors com.example.aigc.exception.BizException
type Biz struct {
	Status  int
	Message string
}

func (e *Biz) Error() string {
	return e.Message
}

func New(status int, msg string) *Biz {
	return &Biz{Status: status, Message: msg}
}

func Wrap(status int, format string, args ...any) *Biz {
	return &Biz{Status: status, Message: fmt.Sprintf(format, args...)}
}
