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
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
