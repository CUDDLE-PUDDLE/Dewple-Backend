package com.dewple.user.service;

import com.dewple.common.enums.VerificationPurpose;
import com.dewple.common.enums.VerificationType;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.Verification;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.port.SmsVerificationPort;
import com.dewple.user.repository.VerificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private SmsVerificationPort smsVerificationPort;

    @InjectMocks
    private VerificationService verificationService;

    private static final String TEST_PHONE = "010-1234-5678";
    private static final String TEST_PHONE_NORMALIZED = "01012345678";
    private static final String TEST_CODE = "A1B2C3";

    @Nested
    @DisplayName("sendPhoneVerificationCode - 인증 코드 발송")
    class SendPhoneVerificationCode {

        @Test
        @DisplayName("성공: 새로운 인증 코드 발송")
        void success() {
            // given
            given(verificationRepository.findLatestByDataAndType(TEST_PHONE_NORMALIZED, VerificationType.PHONE))
                    .willReturn(Optional.empty());
            given(verificationRepository.save(any(Verification.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Verification result = verificationService.sendPhoneVerificationCode(TEST_PHONE, VerificationPurpose.SIGN_UP);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getPublicId()).isNotNull();

            ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationRepository).save(captor.capture());

            Verification saved = captor.getValue();
            assertThat(saved.getTarget()).isEqualTo(TEST_PHONE_NORMALIZED);
            assertThat(saved.getType()).isEqualTo(VerificationType.PHONE);
            assertThat(saved.getPurpose()).isEqualTo(VerificationPurpose.SIGN_UP);
            assertThat(saved.getCode()).hasSize(6);
            assertThat(saved.getCode()).matches("^[A-Z0-9]{6}$");
            assertThat(saved.getIsVerified()).isFalse();

            verify(smsVerificationPort).sendVerificationCode(eq(TEST_PHONE_NORMALIZED), any());
        }

        @Test
        @DisplayName("성공: 하이픈 포함 전화번호도 정규화하여 저장")
        void successWithHyphenatedPhone() {
            // given
            given(verificationRepository.findLatestByDataAndType(TEST_PHONE_NORMALIZED, VerificationType.PHONE))
                    .willReturn(Optional.empty());
            given(verificationRepository.save(any(Verification.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Verification result = verificationService.sendPhoneVerificationCode("010-1234-5678", VerificationPurpose.SIGN_UP);

            // then
            assertThat(result).isNotNull();
            ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationRepository).save(captor.capture());
            assertThat(captor.getValue().getTarget()).isEqualTo(TEST_PHONE_NORMALIZED);
        }

        @Test
        @DisplayName("성공: 60초 이후 재요청 가능")
        void successAfterCooldown() {
            // given
            Verification oldVerification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            ReflectionTestUtils.setField(oldVerification, "createdAt", OffsetDateTime.now().minusSeconds(61));

            given(verificationRepository.findLatestByDataAndType(TEST_PHONE_NORMALIZED, VerificationType.PHONE))
                    .willReturn(Optional.of(oldVerification));
            given(verificationRepository.save(any(Verification.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Verification result = verificationService.sendPhoneVerificationCode(TEST_PHONE, VerificationPurpose.SIGN_UP);

            // then
            assertThat(result).isNotNull();
            verify(smsVerificationPort).sendVerificationCode(eq(TEST_PHONE_NORMALIZED), any());
        }

        @Test
        @DisplayName("실패: 60초 이내 재요청 불가")
        void failWithTooManyRequests() {
            // given
            Verification recentVerification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            ReflectionTestUtils.setField(recentVerification, "createdAt", OffsetDateTime.now().minusSeconds(30));

            given(verificationRepository.findLatestByDataAndType(TEST_PHONE_NORMALIZED, VerificationType.PHONE))
                    .willReturn(Optional.of(recentVerification));

            // when & then
            assertThatThrownBy(() -> verificationService.sendPhoneVerificationCode(TEST_PHONE, VerificationPurpose.SIGN_UP))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.TOO_MANY_VERIFICATION_REQUESTS);
                    });

            verify(verificationRepository, never()).save(any());
            verify(smsVerificationPort, never()).sendVerificationCode(any(), any());
        }

        @Test
        @DisplayName("실패: SMS 발송 오류")
        void failWithSmsSendError() {
            // given
            given(verificationRepository.findLatestByDataAndType(TEST_PHONE_NORMALIZED, VerificationType.PHONE))
                    .willReturn(Optional.empty());
            given(verificationRepository.save(any(Verification.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            doThrow(new RuntimeException("SMS 발송 실패"))
                    .when(smsVerificationPort).sendVerificationCode(any(), any());

            // when & then
            assertThatThrownBy(() -> verificationService.sendPhoneVerificationCode(TEST_PHONE, VerificationPurpose.SIGN_UP))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.SMS_SEND_FAILED);
                    });
        }
    }

    @Nested
    @DisplayName("verifyPhoneCode - 인증 코드 확인")
    class VerifyPhoneCode {

        @Test
        @DisplayName("성공: 올바른 인증 코드로 인증 완료")
        void success() {
            // given
            Verification verification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            UUID publicId = verification.getPublicId();

            given(verificationRepository.findByPublicId(publicId))
                    .willReturn(Optional.of(verification));

            // when
            Verification result = verificationService.verifyPhoneCode(publicId.toString(), TEST_CODE);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getIsVerified()).isTrue();
            assertThat(result.getToken()).isNotNull();
            assertThat(result.getToken()).startsWith("vp_");
        }

        @Test
        @DisplayName("성공: 이미 인증 완료된 경우 기존 토큰 반환")
        void successWithAlreadyVerified() {
            // given
            Verification verification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            verification.markAsVerified();
            String existingToken = verification.getToken();
            UUID publicId = verification.getPublicId();

            given(verificationRepository.findByPublicId(publicId))
                    .willReturn(Optional.of(verification));

            // when
            Verification result = verificationService.verifyPhoneCode(publicId.toString(), TEST_CODE);

            // then
            assertThat(result.getToken()).isEqualTo(existingToken);
        }

        @Test
        @DisplayName("실패: 인증 요청을 찾을 수 없음")
        void failWithNotFound() {
            // given
            UUID randomId = UUID.randomUUID();
            given(verificationRepository.findByPublicId(randomId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> verificationService.verifyPhoneCode(randomId.toString(), TEST_CODE))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.VERIFICATION_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 인증 코드가 만료됨")
        void failWithExpired() {
            // given
            Verification verification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            ReflectionTestUtils.setField(verification, "tokenExpireAt", OffsetDateTime.now().minusMinutes(1));
            UUID publicId = verification.getPublicId();

            given(verificationRepository.findByPublicId(publicId))
                    .willReturn(Optional.of(verification));

            // when & then
            assertThatThrownBy(() -> verificationService.verifyPhoneCode(publicId.toString(), TEST_CODE))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.VERIFICATION_CODE_EXPIRED);
                    });
        }

        @Test
        @DisplayName("실패: 인증 코드가 일치하지 않음")
        void failWithInvalidCode() {
            // given
            Verification verification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            UUID publicId = verification.getPublicId();

            given(verificationRepository.findByPublicId(publicId))
                    .willReturn(Optional.of(verification));

            // when & then
            assertThatThrownBy(() -> verificationService.verifyPhoneCode(publicId.toString(), "WRONG1"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.VERIFICATION_CODE_INVALID);
                    });
        }
    }

    @Nested
    @DisplayName("validateVerificationToken - 본인인증 토큰 검증")
    class ValidateVerificationToken {

        @Test
        @DisplayName("성공: 유효한 본인인증 토큰")
        void success() {
            // given
            Verification verification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            verification.markAsVerified();

            given(verificationRepository.findByToken(verification.getToken()))
                    .willReturn(Optional.of(verification));

            // when
            String phone = verificationService.validateVerificationToken(verification.getToken());

            // then
            assertThat(phone).isEqualTo(TEST_PHONE_NORMALIZED);
        }

        @Test
        @DisplayName("실패: 본인인증 토큰을 찾을 수 없음")
        void failWithNotFound() {
            // given
            String invalidToken = "invalid-token";
            given(verificationRepository.findByToken(invalidToken))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> verificationService.validateVerificationToken(invalidToken))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.VERIFICATION_TOKEN_INVALID);
                    });
        }

        @Test
        @DisplayName("실패: 본인인증 토큰이 만료됨")
        void failWithExpired() {
            // given
            Verification verification = createVerification(TEST_PHONE_NORMALIZED, TEST_CODE);
            verification.markAsVerified();
            ReflectionTestUtils.setField(verification, "tokenExpireAt", OffsetDateTime.now().minusMinutes(1));

            given(verificationRepository.findByToken(verification.getToken()))
                    .willReturn(Optional.of(verification));

            // when & then
            assertThatThrownBy(() -> verificationService.validateVerificationToken(verification.getToken()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.VERIFICATION_TOKEN_EXPIRED);
                    });
        }
    }

    private Verification createVerification(String phone, String code) {
        Verification verification = Verification.builder()
                .type(VerificationType.PHONE)
                .target(phone)
                .code(code)
                .purpose(VerificationPurpose.SIGN_UP)
                .build();
        ReflectionTestUtils.setField(verification, "createdAt", OffsetDateTime.now());
        return verification;
    }
}
