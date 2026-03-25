package com.dewple.organization.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrganizationErrorCode implements ErrorCode {

    ORGANIZATION_NOT_FOUND(7000, HttpStatus.NOT_FOUND, "연합회를 찾을 수 없습니다."),
    ORGANIZATION_NOT_PENDING(7001, HttpStatus.BAD_REQUEST, "승인 대기 상태가 아닙니다."),
    CATEGORY_REQUIRED(7002, HttpStatus.BAD_REQUEST, "카테고리는 최소 1개 필수입니다."),
    CATEGORY_LIMIT_EXCEEDED(7003, HttpStatus.BAD_REQUEST, "카테고리는 최대 3개까지 선택 가능합니다."),
    REGION_REQUIRED(7004, HttpStatus.BAD_REQUEST, "지역은 필수입니다."),
    ORGANIZATION_NOT_APPROVED(7005, HttpStatus.BAD_REQUEST, "승인되지 않은 연합회입니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
