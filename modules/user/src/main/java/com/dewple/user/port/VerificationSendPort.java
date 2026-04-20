package com.dewple.user.port;

import com.dewple.common.enums.VerificationType;

/**
 * 인증 코드 및 임시 비밀번호 발송을 위한 포트 인터페이스
 * - app-api-auth: SQS 메시지 발행 (운영) / 로그 출력 (로컬)
 */
public interface VerificationSendPort {

    void sendVerificationCode(VerificationType type, String target, String code);

    void sendTemporaryPassword(String phone, String temporaryPassword);
}
