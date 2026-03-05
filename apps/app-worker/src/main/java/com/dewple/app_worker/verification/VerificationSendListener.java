package com.dewple.app_worker.verification;

import com.dewple.user.message.VerificationSendMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationSendListener {

    private final SmsSender smsSender;
    private final EmailSender emailSender;

    @SqsListener("${app.sqs.verification-send-queue}")
    public void handle(VerificationSendMessage message) {
        log.info("인증 코드 발송 메시지 수신: type={}, target={}", message.type(), maskTarget(message.target()));

        switch (message.type()) {
            case PHONE -> smsSender.send(message.target(), message.code());
            case EMAIL -> emailSender.send(message.target(), message.code());
        }
    }

    private String maskTarget(String target) {
        if (target == null || target.length() < 4) {
            return "****";
        }
        return target.substring(0, 2) + "****";
    }
}
