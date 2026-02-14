package com.dewple.app_api_auth.auth.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.enums.VerificationPurpose;
import com.dewple.common.enums.VerificationType;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.Verification;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.service.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VerificationService verificationService;

    @Nested
    @DisplayName("POST /auth/verifications/phone - 인증 코드 발송")
    class SendVerificationCode {

        @Test
        @DisplayName("성공: 유효한 전화번호로 인증 코드 발송")
        void success() throws Exception {
            // given
            Verification verification = createVerification();

            given(verificationService.sendPhoneVerificationCode(eq("010-1234-5678"), eq(VerificationPurpose.SIGN_UP)))
                    .willReturn(verification);

            // when & then
            mockMvc.perform(post("/auth/verifications/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "010-1234-5678",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.verificationId").value(verification.getPublicId().toString()))
                    .andExpect(jsonPath("$.result.expiredAt").exists())
                    .andExpect(jsonPath("$.result.resendAfterSeconds").value(60));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 전화번호 형식 (하이픈 없음)")
        void failWithInvalidPhoneFormat() throws Exception {
            mockMvc.perform(post("/auth/verifications/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "01012345678",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 빈 전화번호")
        void failWithEmptyPhone() throws Exception {
            mockMvc.perform(post("/auth/verifications/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: purpose 누락")
        void failWithMissingPurpose() throws Exception {
            mockMvc.perform(post("/auth/verifications/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "010-1234-5678"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 60초 이내 재요청 (429)")
        void failWithTooManyRequests() throws Exception {
            // given
            given(verificationService.sendPhoneVerificationCode(eq("010-1234-5678"), eq(VerificationPurpose.SIGN_UP)))
                    .willThrow(new BusinessException(UserErrorCode.TOO_MANY_VERIFICATION_REQUESTS));

            // when & then
            mockMvc.perform(post("/auth/verifications/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "010-1234-5678",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value(4008));
        }

        @Test
        @DisplayName("실패: SMS 발송 실패 (500)")
        void failWithSmsSendError() throws Exception {
            // given
            given(verificationService.sendPhoneVerificationCode(eq("010-1234-5678"), eq(VerificationPurpose.SIGN_UP)))
                    .willThrow(new BusinessException(UserErrorCode.SMS_SEND_FAILED));

            // when & then
            mockMvc.perform(post("/auth/verifications/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phoneNumber", "010-1234-5678",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(4009));
        }
    }

    @Nested
    @DisplayName("POST /auth/verifications/{verificationId}/confirm - 인증 코드 확인")
    class ConfirmVerificationCode {

        @Test
        @DisplayName("성공: 올바른 인증 코드로 토큰 발급")
        void success() throws Exception {
            // given
            Verification verification = createVerification();
            verification.markAsVerified();
            String verificationId = verification.getPublicId().toString();

            given(verificationService.verifyPhoneCode(eq(verificationId), eq("A1B2C3")))
                    .willReturn(verification);

            // when & then
            mockMvc.perform(post("/auth/verifications/{verificationId}/confirm", verificationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", "A1B2C3"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.verificationToken").value(verification.getToken()));
        }

        @Test
        @DisplayName("실패: 인증 코드 불일치")
        void failWithInvalidCode() throws Exception {
            // given
            String verificationId = UUID.randomUUID().toString();

            given(verificationService.verifyPhoneCode(eq(verificationId), eq("WRONG1")))
                    .willThrow(new BusinessException(UserErrorCode.VERIFICATION_CODE_INVALID));

            // when & then
            mockMvc.perform(post("/auth/verifications/{verificationId}/confirm", verificationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", "WRONG1"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(4002));
        }

        @Test
        @DisplayName("실패: 인증 코드 만료")
        void failWithExpired() throws Exception {
            // given
            String verificationId = UUID.randomUUID().toString();

            given(verificationService.verifyPhoneCode(eq(verificationId), eq("A1B2C3")))
                    .willThrow(new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED));

            // when & then
            mockMvc.perform(post("/auth/verifications/{verificationId}/confirm", verificationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", "A1B2C3"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(4001));
        }

        @Test
        @DisplayName("실패: 인증 요청 미존재")
        void failWithNotFound() throws Exception {
            // given
            String verificationId = UUID.randomUUID().toString();

            given(verificationService.verifyPhoneCode(eq(verificationId), eq("A1B2C3")))
                    .willThrow(new BusinessException(UserErrorCode.VERIFICATION_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/auth/verifications/{verificationId}/confirm", verificationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", "A1B2C3"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(4003));
        }

        @Test
        @DisplayName("실패: 잘못된 코드 형식 (소문자)")
        void failWithInvalidCodeFormat() throws Exception {
            String verificationId = UUID.randomUUID().toString();

            mockMvc.perform(post("/auth/verifications/{verificationId}/confirm", verificationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", "a1b2c3"))))
                    .andExpect(status().isBadRequest());
        }
    }

    private Verification createVerification() {
        Verification verification = Verification.builder()
                .type(VerificationType.PHONE)
                .target("01012345678")
                .code("A1B2C3")
                .purpose(VerificationPurpose.SIGN_UP)
                .build();
        ReflectionTestUtils.setField(verification, "createdAt", OffsetDateTime.now());
        return verification;
    }
}
