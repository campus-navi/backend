package com.campusnavi.backend.auth.dto;

import jakarta.validation.constraints.*;

public record SignUpRequest(
        @NotBlank
        String verifiedToken,

        @NotBlank
        @Size(min = 5, max = 30, message = "아이디는 5~30자 이내로 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9_]+$", message = "영문 소문자, 숫자, 밑줄(_)만 사용 가능합니다.")
        String username,

        @NotBlank
        @Size(min = 8, max = 16, message = "비밀번호는 8~16자 이내로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z\\d!@#$%^&*()]+$", message = "8~16자의 영문,숫자,특수문자를 사용해 주세요.")
        String password,

        @NotBlank
        String nickname,

        @NotNull
        Long departmentId,

        @NotNull
        Integer admissionYear,

        @NotNull
        @Min(1) @Max(4)
        Integer grade
) {
}
