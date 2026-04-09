package com.campusnavi.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST,"a000"),
    INVALID_JSON(HttpStatus.BAD_REQUEST,"a001"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"a002"),
    FORBIDDEN(HttpStatus.FORBIDDEN,"a003"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,"a004"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,"a005"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"a004");


    private final HttpStatus status;
    private final String code;
}
