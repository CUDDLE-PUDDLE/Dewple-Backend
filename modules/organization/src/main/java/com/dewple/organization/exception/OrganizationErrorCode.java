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
    ORGANIZATION_UPDATE_FORBIDDEN(7006, HttpStatus.FORBIDDEN, "연합회 정보를 수정할 권한이 없습니다."),
    DISSOLUTION_ALREADY_REQUESTED(7010, HttpStatus.BAD_REQUEST, "이미 해산 신청된 연합회입니다."),
    DISSOLUTION_REQUEST_FORBIDDEN(7011, HttpStatus.FORBIDDEN, "연합회 해산을 신청할 권한이 없습니다."),
    DISSOLUTION_NOT_REQUESTED(7012, HttpStatus.BAD_REQUEST, "해산 신청 상태가 아닙니다."),
    DISSOLUTION_CANCEL_FORBIDDEN(7013, HttpStatus.FORBIDDEN, "대표만 해산을 취소할 수 있습니다."),
    DISSOLUTION_NOT_IN_GRACE_PERIOD(7014, HttpStatus.BAD_REQUEST, "유예 기간이 아니므로 취소할 수 없습니다."),
    ROLE_NOT_FOUND(7020, HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다."),
    ROLE_NAME_DUPLICATED(7021, HttpStatus.BAD_REQUEST, "이미 존재하는 역할 이름입니다."),
    ROLE_DEFAULT_NOT_MODIFIABLE(7022, HttpStatus.BAD_REQUEST, "기본 역할은 수정할 수 없습니다."),
    ROLE_DEFAULT_NOT_DELETABLE(7023, HttpStatus.BAD_REQUEST, "기본 역할은 삭제할 수 없습니다."),
    ROLE_MANAGE_FORBIDDEN(7024, HttpStatus.FORBIDDEN, "역할을 관리할 권한이 없습니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
