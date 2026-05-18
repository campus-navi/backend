package com.campusnavi.backend.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsernameUpdateRequest(
        @NotBlank
        @Size(min = 5, max = 30, message = "아이디는 5~30자 이내로 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9_]+$", message = "영문 소문자, 숫자, 밑줄(_)만 사용 가능합니다.")
        String username
) {
}
