package com.arun.demo.exception;

public class ServerException extends RuntimeException {
    private String message;

    public ServerException() {
        super();
    }

    public ServerException(String message) {
        super(message);
        this.message = message;
    }
}