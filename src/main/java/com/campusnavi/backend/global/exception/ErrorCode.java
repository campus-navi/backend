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
    CAMPUS_NOT_FOUND(HttpStatus.NOT_FOUND,"b001"),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND,"b002"),

    //JWT 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,"JWT_001"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,"JWT_002"),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED,"JWT_003"),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED,"JWT_004"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,"JWT_005"),

    //이메일 발송 관련
    EMAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR,"EMAIL_001"),

    //회원가입 관련
    DUPLICATE_EMAIL(HttpStatus.CONFLICT,"AUTH_001"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT,"AUTH_007"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT,"AUTH_008"),
    DOMAIN_MISMATCH(HttpStatus.BAD_REQUEST,"AUTH_002"),
    EMAIL_SEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS,"AUTH_003"),
    IP_BLOCKED(HttpStatus.TOO_MANY_REQUESTS,"AUTH_004"),
    EMAIL_CODE_NOT_FOUND(HttpStatus.GONE,"AUTH_005"),
    EMAIL_CODE_INVALID(HttpStatus.BAD_REQUEST,"AUTH_006"),
    EMAIL_VERIFY_BLOCKED(HttpStatus.TOO_MANY_REQUESTS,"AUTH_011"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN,"AUTH_009"),

    //로그인 관련
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,"AUTH_010");

    private final HttpStatus status;
    private final String code;
}
