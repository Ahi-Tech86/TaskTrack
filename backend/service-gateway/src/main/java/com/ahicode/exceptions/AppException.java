package com.ahicode.exceptions;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class AppException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final LocalDateTime timestamp;

    public AppException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
