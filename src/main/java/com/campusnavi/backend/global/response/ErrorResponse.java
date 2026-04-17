package com.campusnavi.backend.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(defaultValue = "false")
        boolean success,
        String code
) {
    public static ErrorResponse of(String code) {
        return new ErrorResponse(false, code);
    }
}
