package com.example.aigc.service;

public class ProviderGatewayException extends RuntimeException {

    private final int statusCode;

    public ProviderGatewayException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
