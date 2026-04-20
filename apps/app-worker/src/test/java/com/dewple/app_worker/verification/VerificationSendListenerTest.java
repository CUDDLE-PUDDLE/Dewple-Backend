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
    @DisplayName("성공: PHONE 인증 코드 메시지 수신 시 SmsSender.send 호출")
    void handlePhoneMessage() {
        // given
        VerificationSendMessage message = VerificationSendMessage.verificationCode(
                VerificationType.PHONE, "01012345678", "123456");

        // when
        verificationSendListener.handle(message);

        // then
        verify(smsSender).send("01012345678", "123456");
    }

    @Test
    @DisplayName("성공: EMAIL 인증 코드 메시지 수신 시 EmailSender.send 호출")
    void handleEmailMessage() {
        // given
        VerificationSendMessage message = VerificationSendMessage.verificationCode(
                VerificationType.EMAIL, "test@example.com", "654321");

        // when
        verificationSendListener.handle(message);

        // then
        verify(emailSender).send("test@example.com", "654321");
    }

    @Test
    @DisplayName("성공: 임시 비밀번호 메시지 수신 시 SmsSender.sendTemporaryPassword 호출")
    void handleTemporaryPasswordMessage() {
        // given
        VerificationSendMessage message = VerificationSendMessage.temporaryPassword(
                "01012345678", "TempPw1!abcd");

        // when
        verificationSendListener.handle(message);

        // then
        verify(smsSender).sendTemporaryPassword("01012345678", "TempPw1!abcd");
    }
}
