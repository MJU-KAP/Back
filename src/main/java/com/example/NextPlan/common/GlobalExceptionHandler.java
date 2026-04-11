package com.example.NextPlan.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, String>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(Map.of(
                        "code", errorCode.getCode(),
                        "message", errorCode.getMessage()
                ));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest() {
        return ResponseEntity.badRequest().body(Map.of(
                "code", ErrorCode.INVALID_REQUEST.getCode(),
                "message", ErrorCode.INVALID_REQUEST.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException() {
        return ResponseEntity.internalServerError().body(Map.of(
                "code", ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "message", ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        ));
    }
}
