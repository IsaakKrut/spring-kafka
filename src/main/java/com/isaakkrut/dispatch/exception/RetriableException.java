package com.isaakkrut.dispatch.exception;

public class RetriableException extends RuntimeException {

    public RetriableException(String message) {
        super(message);
    }

    public RetriableException(Exception exception) {
        super(exception);
    }
}
