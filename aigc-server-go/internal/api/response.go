package api

// Response mirrors Java ApiResponse<T>(int code, String message, T data).
type Response[T any] struct {
	Code    int     `json:"code"`
	Message string  `json:"message"`
	Data    *T      `json:"data"`
}

func OK[T any](data T) Response[T] {
	return Response[T]{Code: 200, Message: "success", Data: &data}
}

func OKNil() Response[struct{}] {
	return Response[struct{}]{Code: 200, Message: "success", Data: nil}
}

func Fail(code int, message string) Response[struct{}] {
	return Response[struct{}]{Code: code, Message: message, Data: nil}
}

func FailT[T any](code int, message string) Response[T] {
	return Response[T]{Code: code, Message: message, Data: nil}
}
