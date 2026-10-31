package com.scbbank.supportportal.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponseDto<T>(
        boolean successful,
        String messageType,
        String role,
        T data,
        String errorCode,
        String errorMessage
) {

    // Success without a role (used by Login and PublicKey, unchanged)
    public static <T> ApiResponseDto<T> success(T data) {
        return success(null, data);
    }

    // Success with a role (used by the portal endpoints)
    public static <T> ApiResponseDto<T> success(String role, T data) {
        return new ApiResponseDto<>(
                true,
                "SUCCESS",
                role,
                data,
                null,
                null
        );
    }

    // Error response with additional details
    public static <T> ApiResponseDto<T> error(
            String errorCode,
            String errorMessage,
            T data
    ) {
        return new ApiResponseDto<>(
                false,
                "ERROR",
                null,
                data,
                errorCode,
                errorMessage
        );
    }

    // Error response without additional details
    public static ApiResponseDto<Void> error(
            String errorCode,
            String errorMessage
    ) {
        return error(errorCode, errorMessage, null);
    }
}