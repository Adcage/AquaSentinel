package com.springboot.service.stream;

public class StreamProviderException extends RuntimeException {

    public StreamProviderException(String message) {
        super(message);
    }

    public StreamProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
