package com.oviro.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends OviroException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
