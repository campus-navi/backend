package com.campusnavi.backend.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @NotBlank
        @Size(min = 8, max = 16, message = "비밀번호는 8~16자 이내로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z\\d!@#$%^&*()]+$", message = "8~16자의 영문,숫자,특수문자를 사용해 주세요.")
        String password
) {
}
