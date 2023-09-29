package com.isaakkrut.dispatch.exception;

public class NotRetriableException extends RuntimeException {

    public NotRetriableException(Exception exception) {
        super(exception);
    }
}
