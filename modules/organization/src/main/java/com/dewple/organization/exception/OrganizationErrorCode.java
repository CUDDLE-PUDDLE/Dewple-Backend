package com.dewple.organization.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrganizationErrorCode implements ErrorCode {

    // 7000: 동아리 연합회 관련 오류
    ORGANIZATION_NOT_FOUND(7001, HttpStatus.NOT_FOUND, "연합회를 찾을 수 없습니다."),
    ORGANIZATION_NOT_PENDING(7002, HttpStatus.BAD_REQUEST, "승인 대기 상태가 아닙니다."),
    CATEGORY_REQUIRED(7003, HttpStatus.BAD_REQUEST, "카테고리는 최소 1개 필수입니다."),
    CATEGORY_LIMIT_EXCEEDED(7004, HttpStatus.BAD_REQUEST, "카테고리는 최대 3개까지 선택 가능합니다."),
    REGION_REQUIRED(7005, HttpStatus.BAD_REQUEST, "지역은 필수입니다."),
    ORGANIZATION_NOT_APPROVED(7006, HttpStatus.BAD_REQUEST, "승인되지 않은 연합회입니다."),
    ORGANIZATION_UPDATE_FORBIDDEN(7007, HttpStatus.FORBIDDEN, "연합회 정보를 수정할 권한이 없습니다."),
    DISSOLUTION_ALREADY_REQUESTED(7008, HttpStatus.BAD_REQUEST, "이미 해산 신청된 연합회입니다."),
    DISSOLUTION_REQUEST_FORBIDDEN(7009, HttpStatus.FORBIDDEN, "연합회 해산을 신청할 권한이 없습니다."),
    DISSOLUTION_NOT_REQUESTED(7010, HttpStatus.BAD_REQUEST, "해산 신청 상태가 아닙니다."),
    DISSOLUTION_CANCEL_FORBIDDEN(7011, HttpStatus.FORBIDDEN, "대표만 해산을 취소할 수 있습니다."),
    DISSOLUTION_NOT_IN_GRACE_PERIOD(7012, HttpStatus.BAD_REQUEST, "유예 기간이 아니므로 취소할 수 없습니다."),
    DISSOLUTION_SANCTION_NOT_CANCELABLE(7013, HttpStatus.BAD_REQUEST, "서비스 관리자의 제재에 의한 삭제는 취소할 수 없습니다."),
    ROLE_NOT_FOUND(7014, HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다."),
    ROLE_NAME_DUPLICATED(7015, HttpStatus.BAD_REQUEST, "이미 존재하는 역할 이름입니다."),
    ROLE_DEFAULT_NOT_MODIFIABLE(7016, HttpStatus.BAD_REQUEST, "기본 역할은 수정할 수 없습니다."),
    ROLE_DEFAULT_NOT_DELETABLE(7017, HttpStatus.BAD_REQUEST, "기본 역할은 삭제할 수 없습니다."),
    ROLE_MANAGE_FORBIDDEN(7018, HttpStatus.FORBIDDEN, "역할을 관리할 권한이 없습니다."),
    REPRESENTATIVE_ROLE_NOT_ASSIGNABLE(7019, HttpStatus.BAD_REQUEST, "대표 역할은 직접 할당할 수 없습니다. 대표 위임을 사용하세요."),
    REPRESENTATIVE_ROLE_NOT_MODIFIABLE(7020, HttpStatus.BAD_REQUEST, "대표 역할의 권한은 불변입니다."),
    DEFAULT_ROLE_MODIFY_FORBIDDEN(7021, HttpStatus.FORBIDDEN, "기본 역할은 대표만 수정할 수 있습니다."),
    MEMBER_NOT_FOUND(7022, HttpStatus.NOT_FOUND, "연합회 멤버를 찾을 수 없습니다."),
    NOT_ORGANIZATION_MEMBER(7023, HttpStatus.FORBIDDEN, "연합회 멤버가 아닙니다."),
    DELEGATE_FORBIDDEN(7024, HttpStatus.FORBIDDEN, "대표만 대표 위임을 할 수 있습니다."),
    DELEGATE_SELF(7025, HttpStatus.BAD_REQUEST, "자기 자신에게 위임할 수 없습니다."),
    PERMISSION_DENIED(7026, HttpStatus.FORBIDDEN, "해당 작업에 대한 권한이 없습니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
