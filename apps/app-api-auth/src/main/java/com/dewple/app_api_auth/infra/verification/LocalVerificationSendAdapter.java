package com.dewple.app_api_auth.infra.verification;

import com.dewple.common.enums.VerificationType;
import com.dewple.user.port.VerificationSendPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용 인증 코드 발송 어댑터
 * - SQS 비활성화 시 인증 코드를 로그로 출력 (실제 발송 없음)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "false", matchIfMissing = true)
public class LocalVerificationSendAdapter implements VerificationSendPort {

    @Override
    public void sendVerificationCode(VerificationType type, String target, String code) {
        log.info("[LOCAL] 인증 코드 발송 (실제 발송 없음): type={}, target={}, code={}", type, target, code);
    }

    @Override
    public void sendTemporaryPassword(String phone, String temporaryPassword) {
        log.info("[LOCAL] 임시 비밀번호 발송 (실제 발송 없음): phone={}, temporaryPassword={}", phone, temporaryPassword);
    }
}
