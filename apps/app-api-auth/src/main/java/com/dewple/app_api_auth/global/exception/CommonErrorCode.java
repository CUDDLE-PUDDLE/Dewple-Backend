package com.dewple.app_api_auth.global.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    /**
     * 1000: 요청 성공 (OK)
     */
    SUCCESS(1000, HttpStatus.OK, "요청에 성공하였습니다."),

    /**
     * 2000: Request 오류 (BAD_REQUEST)
     */
    BAD_REQUEST(2000, HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    URL_NOT_FOUND(2001, HttpStatus.NOT_FOUND, "유효하지 않은 URL 입니다."),
    METHOD_NOT_ALLOWED(2002, HttpStatus.METHOD_NOT_ALLOWED, "해당 URL에서는 지원하지 않는 HTTP Method 입니다."),
    HTTP_MESSAGE_NOT_READABLE(2003, HttpStatus.BAD_REQUEST, "request body 양식에 문제가 있습니다."),

    /**
     * 3000: Server 오류 (INTERNAL_SERVER_ERROR)
     */
    SERVER_ERROR(3000, HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생하였습니다."),
    DB_TEMPORARY_UNAVAILABLE(3001, HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 데이터를 처리할 수 없습니다."),
    DB_CONSTRAINT_VIOLATE(3004, HttpStatus.BAD_REQUEST, "DB 무결성에 적합하지 않습니다."),

    /**
     * 4000: Authentication 오류
     */
    UNSUPPORTED_TOKEN_TYPE(4001, HttpStatus.UNAUTHORIZED, "지원되지 않는 토큰 형식입니다."),
    INVALID_TOKEN(4002, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(4003, HttpStatus.UNAUTHORIZED, "만료된 token 입니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
