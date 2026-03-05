package com.dewple.app_worker.verification;

import com.dewple.common.enums.VerificationType;
import com.dewple.user.message.VerificationSendMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationSendListenerTest {

    @Mock
    private SmsSender smsSender;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private VerificationSendListener verificationSendListener;

    @Test
    @DisplayName("성공: PHONE 메시지 수신 시 SmsSender 호출")
    void handlePhoneMessage() {
        // given
        VerificationSendMessage message = new VerificationSendMessage(
                VerificationType.PHONE, "01012345678", "123456");

        // when
        verificationSendListener.handle(message);

        // then
        verify(smsSender).send("01012345678", "123456");
    }

    @Test
    @DisplayName("성공: EMAIL 메시지 수신 시 EmailSender 호출")
    void handleEmailMessage() {
        // given
        VerificationSendMessage message = new VerificationSendMessage(
                VerificationType.EMAIL, "test@example.com", "654321");

        // when
        verificationSendListener.handle(message);

        // then
        verify(emailSender).send("test@example.com", "654321");
    }
}
