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
    INVITE_CODE_NOT_FOUND(5015, HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    ACTIVITY_NOT_PRIVATE(5016, HttpStatus.BAD_REQUEST, "비공개 모임만 초대 코드를 사용할 수 있습니다."),
    ACTIVITY_INVITE_PERMISSION_DENIED(5017, HttpStatus.FORBIDDEN, "초대 코드 조회 권한이 없습니다."),
    ALREADY_PARTICIPANT(5018, HttpStatus.CONFLICT, "이미 참가 신청한 모임입니다."),
    ACTIVITY_FULL(5019, HttpStatus.CONFLICT, "모임 정원이 가득 찼습니다."),
    CANNOT_JOIN_OWN_ACTIVITY(5020, HttpStatus.BAD_REQUEST, "본인이 생성한 모임에는 참가할 수 없습니다."),
    ACTIVITY_NOT_USER_CREATED(5021, HttpStatus.BAD_REQUEST, "개인 모임만 초대 코드를 사용할 수 있습니다."),
    INTEREST_LIMIT_EXCEEDED(5022, HttpStatus.BAD_REQUEST, "관심 모임은 최대 20개까지 설정 가능합니다."),
    LEADER_ACTIVITY_LIMIT_EXCEEDED(5023, HttpStatus.BAD_REQUEST, "모임장으로서 동시 운영 가능한 모임은 최대 10개입니다."),
    CANCEL_DEADLINE_EXCEEDED(5024, HttpStatus.BAD_REQUEST, "참여 취소 가능 기한이 지났습니다."),
    ACTIVITY_ALREADY_STARTED(5025, HttpStatus.BAD_REQUEST, "이미 시작된 모임은 참여를 취소할 수 없습니다."),
    PARTICIPANT_NOT_CONFIRMED(5026, HttpStatus.BAD_REQUEST, "참여 확정 상태가 아닙니다."),
    MANAGER_LIMIT_EXCEEDED(5027, HttpStatus.BAD_REQUEST, "모임관리자는 최대 50명까지 초대 가능합니다."),
    NOT_ACTIVITY_LEADER(5028, HttpStatus.FORBIDDEN, "모임장만 수행할 수 있습니다."),
    ALREADY_MANAGER(5029, HttpStatus.CONFLICT, "이미 모임관리자로 등록되어 있습니다."),
    MANAGER_NOT_FOUND(5030, HttpStatus.NOT_FOUND, "해당 모임관리자를 찾을 수 없습니다."),
    CANNOT_REMOVE_LEADER(5031, HttpStatus.BAD_REQUEST, "모임장은 제거할 수 없습니다."),
    ACTIVITY_ALREADY_CANCELLED(5032, HttpStatus.BAD_REQUEST, "이미 취소된 모임입니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
