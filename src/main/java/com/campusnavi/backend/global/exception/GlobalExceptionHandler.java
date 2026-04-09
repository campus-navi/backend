package com.campusnavi.backend.global.exception;

import com.campusnavi.backend.global.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e){
        ErrorCode code = e.getErrorCode();

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        ErrorCode code = ErrorCode.INVALID_INPUT;

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
        ErrorCode code = ErrorCode.INVALID_JSON;

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e){
        ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e){
        ErrorCode code = ErrorCode.INVALID_PARAM;

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e){
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception",e);

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode()));
    }
}
