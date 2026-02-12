package com.dewple.app_api_auth.global.response;

import com.dewple.app_api_auth.global.exception.WebErrorCode;
import com.dewple.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"code", "status", "message", "result"})
public class ApiResponse<T> {

    private final int code;
    private final int status;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    private ApiResponse(int code, int status, String message, T result) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.result = result;
    }

    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(
                WebErrorCode.SUCCESS.getCode(),
                WebErrorCode.SUCCESS.getHttpStatus().value(),
                WebErrorCode.SUCCESS.getMessage(),
                result
        );
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(
                WebErrorCode.SUCCESS.getCode(),
                WebErrorCode.SUCCESS.getHttpStatus().value(),
                WebErrorCode.SUCCESS.getMessage(),
                null
        );
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage(),
                null
        );
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String overrideMessage) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                overrideMessage,
                null
        );
    }

    public static ApiResponse<Void> error(int code, int status, String message) {
        return new ApiResponse<>(code, status, message, null);
    }
}
