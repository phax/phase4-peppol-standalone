package com.mysupply.phase4.peppolstandalone.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class HttpForbiddenException extends RuntimeException {
    public HttpForbiddenException() {
        super("Forbidden");
    }

    public HttpForbiddenException(String message) {
        super(message);
    }
}
