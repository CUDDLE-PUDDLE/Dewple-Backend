package com.dewple.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    USER_NOT_FOUND(9001, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(9002, HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    REGION_NOT_FOUND(9003, HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
