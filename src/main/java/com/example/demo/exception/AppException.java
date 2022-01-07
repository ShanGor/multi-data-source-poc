package com.example.demo.exception;

import lombok.Data;

@Data
public class AppException extends RuntimeException{
    private int httpStatus;
    public AppException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
