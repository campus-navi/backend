package com.campusnavi.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String code
) {
    public static <T> ApiResponse <T> ok(T data){
        return new ApiResponse<>(true,data,null);
    }

    public static <T> ApiResponse <T> ok(){
        return new ApiResponse<>(true,null,null);
    }

    public static ApiResponse<Void> error(String code){
        return new ApiResponse<>(false,null,code);
    }
}
