package com.dewple.common.response;

import com.dewple.common.response.status.ResponseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import static com.dewple.common.response.status.BaseExceptionResponseStatus.SUCCESS;

@Getter
@JsonPropertyOrder({"code", "status", "message", "result"})
public class BaseResponse<T> {

    private final int code;
    private final int status;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    private BaseResponse(int code, int status, String message, T result) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.result = result;
    }

    /**
     * 성공 응답 (데이터 포함)
     */
    public static <T> BaseResponse<T> ok(T result) {
        return new BaseResponse<>(
                SUCCESS.getCode(),
                SUCCESS.getStatus().value(),
                SUCCESS.getMessage(),
                result
        );
    }

    /**
     * 성공 응답 (데이터 없음)
     */
    public static BaseResponse<Void> ok() {
        return new BaseResponse<>(
                SUCCESS.getCode(),
                SUCCESS.getStatus().value(),
                SUCCESS.getMessage(),
                null
        );
    }

    /**
     * 에러 응답 (ResponseStatus 기반)
     */
    public static BaseResponse<Void> error(ResponseStatus responseStatus) {
        return new BaseResponse<>(
                responseStatus.getCode(),
                responseStatus.getStatus().value(),
                responseStatus.getMessage(),
                null
        );
    }

    /**
     * 에러 응답 (메시지 오버라이드)
     */
    public static BaseResponse<Void> error(ResponseStatus responseStatus, String overrideMessage) {
        return new BaseResponse<>(
                responseStatus.getCode(),
                responseStatus.getStatus().value(),
                overrideMessage,
                null
        );
    }

    /**
     * 에러 응답 (코드, 상태, 메시지 직접 지정)
     */
    public static BaseResponse<Void> error(int code, int status, String message) {
        return new BaseResponse<>(code, status, message, null);
    }
}
