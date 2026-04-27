package com.example.aigcspringai.exception;

public class InvalidRequestException extends TextGenerationException {

    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", 400, message, false, false);
    }
}
