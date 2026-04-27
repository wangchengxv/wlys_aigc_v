package com.example.aigcspringai.exception;

public class TextGenerationException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final boolean retryable;
    private final boolean fallbackAllowed;

    public TextGenerationException(
            String errorCode,
            int httpStatus,
            String message,
            boolean retryable,
            boolean fallbackAllowed,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
        this.fallbackAllowed = fallbackAllowed;
    }

    public TextGenerationException(
            String errorCode,
            int httpStatus,
            String message,
            boolean retryable,
            boolean fallbackAllowed
    ) {
        this(errorCode, httpStatus, message, retryable, fallbackAllowed, null);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public boolean isFallbackAllowed() {
        return fallbackAllowed;
    }
}
