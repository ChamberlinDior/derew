package com.oviro.exception;

import org.springframework.http.HttpStatus;

public class OviroException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public OviroException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
