package com.campusnavi.backend.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentNumberUpdateRequest(
        @NotNull
        @Min(value = 1900, message = "입학년도는 1900~2099 사이로 입력해주세요.")
        @Max(value = 2099, message = "입학년도는 1900~2099 사이로 입력해주세요.")
        Integer admissionYear,
        @NotBlank
        @Size(min = 6, max = 10, message = "학번은 6~10자 이내로 입력해주세요.")
        @Pattern(regexp = "^\\d+$", message = "학번은 숫자만 입력 가능합니다.")
        String studentNumber
) {
}
