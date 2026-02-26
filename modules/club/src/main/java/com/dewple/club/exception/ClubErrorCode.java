package com.dewple.club.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ClubErrorCode implements ErrorCode {

    /**
     * 6000: Club 관련 오류
     */
    NOT_CLUB_MEMBER(6001, HttpStatus.FORBIDDEN, "해당 동아리의 활동 중인 멤버가 아닙니다."),
    INSUFFICIENT_PERMISSION(6002, HttpStatus.FORBIDDEN, "해당 작업에 대한 권한이 없습니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
