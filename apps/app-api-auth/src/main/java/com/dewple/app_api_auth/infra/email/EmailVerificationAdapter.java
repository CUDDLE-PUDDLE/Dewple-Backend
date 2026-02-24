package com.dewple.app_api_auth.infra.email;

import com.dewple.user.port.EmailVerificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Component
public class EmailVerificationAdapter implements EmailVerificationPort {

    private static final String SUBJECT = "[듀플] 이메일 인증 코드";
    private static final String BODY_TEMPLATE = "[듀플] 인증번호 [%s]를 입력해주세요.";

    private final SesClient sesClient;
    private final String senderEmail;

    public EmailVerificationAdapter(
            SesClient sesClient,
            @Value("${ses.sender-email:}") String senderEmail
    ) {
        this.sesClient = sesClient;
        this.senderEmail = senderEmail;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        if (senderEmail == null || senderEmail.isBlank()) {
            log.warn("SES 발신 이메일이 설정되지 않아 실제 발송을 건너뜁니다. email={}, code={}", maskEmail(email), code);
            return;
        }

        SendEmailRequest request = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(Destination.builder()
                        .toAddresses(email)
                        .build())
                .message(Message.builder()
                        .subject(Content.builder()
                                .data(SUBJECT)
                                .charset("UTF-8")
                                .build())
                        .body(Body.builder()
                                .text(Content.builder()
                                        .data(String.format(BODY_TEMPLATE, code))
                                        .charset("UTF-8")
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
        log.info("이메일 발송 완료: email={}", maskEmail(email));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return "**@" + parts[1];
        }
        return local.substring(0, 2) + "****@" + parts[1];
    }
}
