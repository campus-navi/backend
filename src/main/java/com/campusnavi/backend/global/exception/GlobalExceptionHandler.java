package com.campusnavi.backend.global.exception;

import com.campusnavi.backend.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e){
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code.name()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        ErrorCode code = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code.name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
        ErrorCode code = ErrorCode.INVALID_JSON;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code.name()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e){
        ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code.name()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e){
        ErrorCode code = ErrorCode.INVALID_PARAM;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code.name()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e){
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception", e);
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code.name()));
    }
}
