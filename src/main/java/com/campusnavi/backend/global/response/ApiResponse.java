package com.campusnavi.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        @Schema(nullable = true)
        T data
) {
    public static <T> ApiResponse<T> ok(T data){
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<T> ok(){
        return new ApiResponse<>(true, null);
    }
}
