package com.dewple.app_worker.verification;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsSender {

    private static final String MESSAGE_TEMPLATE = "[듀플] 인증번호 [%s]를 입력해주세요.";

    private final String apiKey;
    private final String apiSecret;
    private final String senderPhone;

    private DefaultMessageService messageService;

    public SmsSender(
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret,
            @Value("${solapi.sender-phone:}") String senderPhone
    ) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.senderPhone = senderPhone;
    }

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
            log.info("SOLAPI MessageService 초기화 완료");
        } else {
            log.warn("SOLAPI 설정이 없어 SMS 발송이 비활성화됩니다.");
        }
    }

    public void send(String phone, String code) {
        if (messageService == null) {
            log.warn("SOLAPI 설정이 없어 실제 발송을 건너뜁니다. phone={}, code={}", maskPhone(phone), code);
            return;
        }

        Message message = new Message();
        message.setFrom(senderPhone);
        message.setTo(phone);
        message.setText(String.format(MESSAGE_TEMPLATE, code));

        try {
            messageService.sendOne(new SingleMessageSendingRequest(message));
            log.info("SMS 발송 완료: phone={}", maskPhone(phone));
        } catch (Exception e) {
            log.error("SMS 발송 실패: phone={}", maskPhone(phone), e);
            throw new RuntimeException("SMS 발송 실패", e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
