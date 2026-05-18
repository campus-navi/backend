package com.campusnavi.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //공통, 코드는 합의 전 임의값
    INVALID_INPUT(HttpStatus.BAD_REQUEST),
    INVALID_JSON(HttpStatus.BAD_REQUEST),
    INVALID_PARAM(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    //대학 조회 관련
    CAMPUS_NOT_FOUND(HttpStatus.NOT_FOUND),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND),

    //JWT 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),

    //이메일 발송 관련
    EMAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR),

    //회원가입 관련
    DUPLICATE_EMAIL(HttpStatus.CONFLICT),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT),
    DOMAIN_MISMATCH(HttpStatus.BAD_REQUEST),
    EMAIL_SEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS),
    IP_BLOCKED(HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_CODE_NOT_FOUND(HttpStatus.GONE),
    EMAIL_CODE_INVALID(HttpStatus.BAD_REQUEST),
    EMAIL_VERIFY_BLOCKED(HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),

    //로그인 관련
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),

    //파일 업로드 관련
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_SIZE_EXCEEDED(HttpStatus.CONTENT_TOO_LARGE),

    //회원 관련
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND),

    //커뮤니티 관련
    POST_NOT_FOUND(HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST),

    //태그 관련
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND),

    //크롤링 관련
    INVALID_PARSER_TYPE(HttpStatus.BAD_REQUEST),

    //공식정보 관련
    OFFICIAL_POST_NOT_FOUND(HttpStatus.NOT_FOUND),
    OFFICIAL_POST_NOT_READY(HttpStatus.TOO_EARLY),
    OFFICIAL_POST_DEADLINE_REQUIRED(HttpStatus.BAD_REQUEST),
    OFFICIAL_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND),

    //활동 알림 관련
    ACTIVITY_NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND),

    //스크랩 폴더 관련
    SCRAP_FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    SCRAP_FOLDER_NAME_DUPLICATE(HttpStatus.CONFLICT);

    private final HttpStatus status;
}
