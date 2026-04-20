package com.dewple.app_api_auth.infra.verification;

import com.dewple.common.enums.VerificationType;
import com.dewple.user.message.VerificationSendMessage;
import com.dewple.user.port.VerificationSendPort;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SQS를 통한 인증 코드 발송 어댑터
 * - VerificationSendMessage를 SQS 큐에 발행
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true")
public class SqsVerificationSendAdapter implements VerificationSendPort {

    private final SqsTemplate sqsTemplate;
    private final String queueName;

    public SqsVerificationSendAdapter(
            SqsTemplate sqsTemplate,
            @Value("${app.sqs.verification-send-queue}") String queueName
    ) {
        this.sqsTemplate = sqsTemplate;
        this.queueName = queueName;
    }

    @Override
    public void sendVerificationCode(VerificationType type, String target, String code) {
        VerificationSendMessage message = VerificationSendMessage.verificationCode(type, target, code);
        sqsTemplate.send(queueName, message);
        log.info("SQS 인증 코드 메시지 발행: type={}, target={}", type, target);
    }

    @Override
    public void sendTemporaryPassword(String phone, String temporaryPassword) {
        VerificationSendMessage message = VerificationSendMessage.temporaryPassword(phone, temporaryPassword);
        sqsTemplate.send(queueName, message);
        log.info("SQS 임시 비밀번호 메시지 발행: phone={}", phone);
    }
}
