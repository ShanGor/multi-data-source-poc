package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlers {
    @ExceptionHandler(AppException.class)
    public ResponseEntity handleValidationExceptions(
            AppException e) {
        return new ResponseEntity(e.getMessage(),  HttpStatus.valueOf(e.getHttpStatus()));
    }
}
