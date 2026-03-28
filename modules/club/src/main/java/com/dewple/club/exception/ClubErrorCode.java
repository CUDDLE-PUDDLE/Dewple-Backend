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
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
