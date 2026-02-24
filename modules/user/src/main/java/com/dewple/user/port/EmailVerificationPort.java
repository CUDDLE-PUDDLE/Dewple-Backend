package com.dewple.user.port;

/**
 * 이메일 인증 발송을 위한 포트 인터페이스
 * - AWS SES를 사용하여 구현
 */
public interface EmailVerificationPort {

    void sendVerificationCode(String email, String code);
}
