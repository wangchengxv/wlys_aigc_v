package com.example.aigcspringai.exception;

public class ClientBuildException extends TextGenerationException {

    public ClientBuildException(String message, Throwable cause) {
        super("CLIENT_BUILD_ERROR", 500, message, false, true, cause);
    }

    public ClientBuildException(String message) {
        super("CLIENT_BUILD_ERROR", 500, message, false, true);
    }
}
