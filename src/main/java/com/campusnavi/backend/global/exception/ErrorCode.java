package com.campusnavi.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //공통, 코드는 합의 전 임의값
    INVALID_INPUT(HttpStatus.BAD_REQUEST,"a000"),
    INVALID_JSON(HttpStatus.BAD_REQUEST,"a001"),
    INVALID_PARAM(HttpStatus.BAD_REQUEST,"a002"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"a003"),
    FORBIDDEN(HttpStatus.FORBIDDEN,"a004"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,"a005"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,"a006"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"a007"),

    //대학 조회 관련
    CAMPUS_NOT_FOUND(HttpStatus.NOT_FOUND,"b001");


    private final HttpStatus status;
    private final String code;
}
