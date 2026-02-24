package com.dewple.user.port;

/**
 * SMS 인증 발송을 위한 포트 인터페이스
 * - SOLAPI를 사용하여 구현
 */
public interface SmsVerificationPort {

    void sendVerificationCode(String phone, String code);
}
