package com.dewple.user.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 4000: 인증 관련 오류
    VERIFICATION_CODE_EXPIRED(4001, HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다."),
    VERIFICATION_CODE_INVALID(4002, HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다."),
    VERIFICATION_NOT_FOUND(4003, HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."),
    VERIFICATION_ALREADY_VERIFIED(4004, HttpStatus.BAD_REQUEST, "이미 인증이 완료되었습니다."),
    VERIFICATION_TOKEN_INVALID(4005, HttpStatus.BAD_REQUEST, "유효하지 않은 인증 토큰입니다."),
    VERIFICATION_TOKEN_EXPIRED(4006, HttpStatus.BAD_REQUEST, "인증 토큰이 만료되었습니다."),
    VERIFICATION_REQUIRED(4007, HttpStatus.BAD_REQUEST, "전화번호 인증이 필요합니다."),
    TOO_MANY_VERIFICATION_REQUESTS(4008, HttpStatus.TOO_MANY_REQUESTS, "인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    SMS_SEND_FAILED(4009, HttpStatus.INTERNAL_SERVER_ERROR, "SMS 발송에 실패했습니다."),
    EMAIL_SEND_FAILED(4010, HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),

    // 4100: 회원가입 관련 오류
    PHONE_ALREADY_EXISTS(4101, HttpStatus.CONFLICT, "이미 가입된 전화번호입니다."),
    USER_ID_ALREADY_EXISTS(4102, HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    NICKNAME_ALREADY_EXISTS(4103, HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    EMAIL_ALREADY_EXISTS(4104, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    WITHDRAWAL_COOLDOWN(4105, HttpStatus.BAD_REQUEST, "탈퇴 후 7일이 지나야 재가입이 가능합니다."),
    PASSWORD_CONFIRM_MISMATCH(4106, HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),

    // 4200: 로그인 관련 오류
    USER_NOT_FOUND(4201, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PASSWORD_MISMATCH(4202, HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    USER_INACTIVE(4203, HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
