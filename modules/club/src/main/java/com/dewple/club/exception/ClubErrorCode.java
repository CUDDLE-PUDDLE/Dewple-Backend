package com.dewple.club.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ClubErrorCode implements ErrorCode {

    // 6000: 동아리 관련 오류
    CLUB_NOT_FOUND(6001, HttpStatus.NOT_FOUND, "동아리를 찾을 수 없습니다."),
    NOT_CLUB_MEMBER(6002, HttpStatus.FORBIDDEN, "해당 동아리의 멤버가 아닙니다."),
    CLUB_PERMISSION_DENIED(6003, HttpStatus.FORBIDDEN, "해당 권한이 없습니다."),
    CLUB_CATEGORY_REQUIRED(6004, HttpStatus.BAD_REQUEST, "카테고리는 최소 1개 필수입니다."),
    CLUB_CATEGORY_LIMIT_EXCEEDED(6005, HttpStatus.BAD_REQUEST, "카테고리는 최대 3개까지 선택 가능합니다."),
    CLUB_REGION_REQUIRED(6006, HttpStatus.BAD_REQUEST, "지역은 필수입니다."),
    CLUB_PRESIDENT_LIMIT_EXCEEDED(6007, HttpStatus.BAD_REQUEST, "회장으로서 동시 운영 가능한 동아리는 최대 5개입니다."),
    ACTIVE_RECRUITMENT_EXISTS(6008, HttpStatus.BAD_REQUEST, "활성 모집 공고가 있을 때는 동아리 설정을 변경할 수 없습니다."),
    VERIFICATION_REQUIRED_FOR_TAG(6009, HttpStatus.BAD_REQUEST, "본인인증 필수 동아리에서만 연령대/성별을 설정할 수 있습니다."),
    DELETION_ALREADY_IN_PROGRESS(6010, HttpStatus.BAD_REQUEST, "이미 삭제 투표가 진행 중이거나 승인된 상태입니다."),
    DELETION_NOT_VOTING(6011, HttpStatus.BAD_REQUEST, "삭제 투표 진행 중이 아닙니다."),
    DELETION_VOTE_NOT_FOUND(6012, HttpStatus.NOT_FOUND, "투표 대상이 아닙니다."),
    DELETION_NOT_APPROVED(6013, HttpStatus.BAD_REQUEST, "삭제 승인 상태가 아닙니다."),
    DELETION_CANCEL_FORBIDDEN(6014, HttpStatus.FORBIDDEN, "회장만 삭제를 취소할 수 있습니다."),
    DELETION_SANCTION_NOT_CANCELABLE(6015, HttpStatus.BAD_REQUEST, "서비스 관리자의 제재에 의한 삭제는 취소할 수 없습니다."),
    DELETION_GRACE_PERIOD_EXPIRED(6016, HttpStatus.BAD_REQUEST, "유예 기간이 만료되어 취소할 수 없습니다."),
    ROLE_NOT_FOUND(6020, HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다."),
    ROLE_NAME_DUPLICATED(6021, HttpStatus.BAD_REQUEST, "이미 존재하는 역할 이름입니다."),
    ROLE_MANAGE_FORBIDDEN(6022, HttpStatus.FORBIDDEN, "역할을 관리할 권한이 없습니다."),
    PRESIDENT_ROLE_NOT_MODIFIABLE(6023, HttpStatus.BAD_REQUEST, "회장 역할의 권한은 불변입니다."),
    DEFAULT_ROLE_MODIFY_FORBIDDEN(6024, HttpStatus.FORBIDDEN, "기본 역할은 회장만 수정할 수 있습니다."),
    ROLE_DEFAULT_NOT_DELETABLE(6025, HttpStatus.BAD_REQUEST, "기본 역할은 삭제할 수 없습니다."),
    PRESIDENT_ROLE_NOT_ASSIGNABLE(6026, HttpStatus.BAD_REQUEST, "회장 역할은 직접 할당할 수 없습니다. 회장 위임을 사용하세요."),
    MEMBER_NOT_FOUND(6027, HttpStatus.NOT_FOUND, "동아리 멤버를 찾을 수 없습니다."),
    DELEGATE_FORBIDDEN(6028, HttpStatus.FORBIDDEN, "회장만 회장 위임을 할 수 있습니다."),
    DELEGATE_SELF(6029, HttpStatus.BAD_REQUEST, "자기 자신에게 위임할 수 없습니다."),
    ALREADY_CLUB_MEMBER(6030, HttpStatus.BAD_REQUEST, "이미 동아리 멤버입니다."),
    ACTIVITY_ID_REQUIRED(6031, HttpStatus.BAD_REQUEST, "GUEST 초대 시 모임 지정은 필수입니다."),
    NOT_GUEST_STATUS(6032, HttpStatus.BAD_REQUEST, "GUEST 상태의 멤버만 승격할 수 있습니다."),
    ACTIVITY_END_DATE_REQUIRED(6033, HttpStatus.BAD_REQUEST, "활동 기한은 필수입니다."),
    GENERATION_NOT_FOUND(6034, HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다."),
    KICK_ALREADY_IN_PROGRESS(6040, HttpStatus.BAD_REQUEST, "이미 내보내기 투표가 진행 중입니다."),
    KICK_VOTE_NOT_FOUND(6041, HttpStatus.NOT_FOUND, "투표 대상이 아닙니다."),
    KICK_NOT_IN_PROGRESS(6042, HttpStatus.BAD_REQUEST, "내보내기 투표가 진행 중이 아닙니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
