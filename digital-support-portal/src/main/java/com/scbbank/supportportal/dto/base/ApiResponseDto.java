
package com.scbbank.supportportal.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponseDto<T>(
        boolean successful,
        String messageType,
        T data,
        String errorCode,
        String errorMessage
) {

// Creates a successful API response
public static <T> ApiResponseDto<T> success(T data) {
    return new ApiResponseDto<>(
            true,
            "SUCCESS",
            data,
            null,
            null
    );
}

// Creates an error response with additional details
public static <T> ApiResponseDto<T> error(
        String errorCode,
        String errorMessage,
        T data
) {
    return new ApiResponseDto<>(
            false,
            "ERROR",
            data,
            errorCode,
            errorMessage
    );
}

// Creates an error response without additional details
public static ApiResponseDto<Void> error(
        String errorCode,
        String errorMessage
) {
    return error(errorCode, errorMessage, null);
}
}
