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
        log.info("발송 메시지 수신: kind={}, type={}, target={}",
                message.kind(), message.type(), maskTarget(message.target()));

        switch (message.kind()) {
            case VERIFICATION_CODE -> dispatchVerificationCode(message);
            case TEMPORARY_PASSWORD -> smsSender.sendTemporaryPassword(message.target(), message.payload());
        }
    }

    private void dispatchVerificationCode(VerificationSendMessage message) {
        switch (message.type()) {
            case PHONE -> smsSender.send(message.target(), message.payload());
            case EMAIL -> emailSender.send(message.target(), message.payload());
        }
    }

    private String maskTarget(String target) {
        if (target == null || target.length() < 4) {
            return "****";
        }
        return target.substring(0, 2) + "****";
    }
}
