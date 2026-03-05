package com.dewple.activity.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ActivityErrorCode implements ErrorCode {

    // 5000: 모임 관련 오류
    ACTIVITY_NOT_FOUND(5000, HttpStatus.NOT_FOUND, "모임을 찾을 수 없습니다."),
    ACTIVITY_END_BEFORE_START(5001, HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간 이후여야 합니다."),
    ACTIVITY_START_IN_PAST(5002, HttpStatus.BAD_REQUEST, "시작 시간은 현재 시간 이후여야 합니다."),
    ACTIVITY_ALREADY_INACTIVE(5003, HttpStatus.FORBIDDEN, "이미 삭제된 모임입니다."),
    ACTIVITY_DELETE_PERMISSION_DENIED(5004, HttpStatus.FORBIDDEN, "모임 삭제 권한이 없습니다."),
    CATEGORY_NOT_FOUND(5005, HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    REGION_NOT_FOUND(5006, HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
    ACTIVITY_PARTICIPANT_VIEW_PERMISSION_DENIED(5007, HttpStatus.FORBIDDEN, "지원자 조회 권한이 없습니다."),
    PARTICIPANT_NOT_FOUND(5008, HttpStatus.NOT_FOUND, "해당 지원자를 찾을 수 없습니다."),
    ACTIVITY_PARTICIPANT_MANAGE_PERMISSION_DENIED(5009, HttpStatus.FORBIDDEN, "지원자 관리 권한이 없습니다."),
    INVALID_PARTICIPANT_STATUS(5010, HttpStatus.BAD_REQUEST, "유효하지 않은 지원 상태입니다."),
    PARTICIPANT_NOT_APPROVED(5011, HttpStatus.BAD_REQUEST, "참여 확정 상태가 아닌 지원자입니다."),
    INVALID_PARTICIPATION_RESPONSE(5012, HttpStatus.BAD_REQUEST, "참여 응답은 CONFIRMED 또는 DECLINED만 가능합니다."),
    ACTIVITY_INTEREST_ALREADY_EXISTS(5013, HttpStatus.CONFLICT, "이미 관심 모임으로 등록되어 있습니다."),
    ACTIVITY_INTEREST_NOT_FOUND(5014, HttpStatus.NOT_FOUND, "관심 모임으로 등록되지 않은 모임입니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
