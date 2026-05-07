package com.example.aigc.exception;

public class BizException extends RuntimeException {

    private final int status;
    private final ErrorCode code;

    public BizException(int status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public BizException(int status, String message) {
        super(message);
        this.status = status;
        this.code = ErrorCode.BAD_REQUEST;
    }

    public int getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }
}