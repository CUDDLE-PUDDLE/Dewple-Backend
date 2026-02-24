package com.dewple.app_api_auth.auth.controller;

import com.dewple.app_api_auth.global.config.JwtProperties;
import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.app_api_auth.global.security.JwtTokenProvider;
import com.dewple.common.entity.User;
import com.dewple.common.enums.VerificationPurpose;
import com.dewple.common.enums.VerificationType;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.RefreshToken;
import com.dewple.user.entity.Verification;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.service.RefreshTokenService;
import com.dewple.user.service.UserService;
import com.dewple.user.service.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VerificationService verificationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtProperties jwtProperties;

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
    @DisplayName("POST /auth/verifications/email - 이메일 인증 코드 발송")
    class SendEmailVerificationCode {

        @Test
        @DisplayName("성공: 유효한 이메일로 인증 코드 발송")
        void success() throws Exception {
            // given
            Verification verification = createEmailVerification();

            given(verificationService.sendEmailVerificationCode(eq("user@example.com"), eq(VerificationPurpose.SIGN_UP)))
                    .willReturn(verification);

            // when & then
            mockMvc.perform(post("/auth/verifications/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", "user@example.com",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.verificationId").value(verification.getPublicId().toString()))
                    .andExpect(jsonPath("$.result.expiredAt").exists())
                    .andExpect(jsonPath("$.result.resendAfterSeconds").value(60));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 이메일 형식")
        void failWithInvalidEmailFormat() throws Exception {
            mockMvc.perform(post("/auth/verifications/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", "invalid-email",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 이메일 누락")
        void failWithMissingEmail() throws Exception {
            mockMvc.perform(post("/auth/verifications/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 이메일 발송 실패 (500)")
        void failWithEmailSendError() throws Exception {
            // given
            given(verificationService.sendEmailVerificationCode(eq("user@example.com"), eq(VerificationPurpose.SIGN_UP)))
                    .willThrow(new BusinessException(UserErrorCode.EMAIL_SEND_FAILED));

            // when & then
            mockMvc.perform(post("/auth/verifications/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", "user@example.com",
                                    "purpose", "SIGN_UP"
                            ))))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(4010));
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

    @Nested
    @DisplayName("POST /auth/signup - 회원가입")
    class Signup {

        @Test
        @DisplayName("성공: 유효한 정보로 회원가입")
        void success() throws Exception {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userService.signup(eq("vp_test-token"), eq("홍길동"), eq("dewple123"), eq("Password1!")))
                    .willReturn(user);
            given(jwtTokenProvider.generateAccessToken(1L)).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");

            // when & then
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "verificationToken", "vp_test-token",
                                    "name", "홍길동",
                                    "userId", "dewple123",
                                    "password", "Password1!",
                                    "passwordConfirm", "Password1!"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result").doesNotExist())
                    .andExpect(header().string("Authorization", "Bearer access-token"))
                    .andExpect(header().string("Authorization-Refresh", "Bearer refresh-token"));
        }

        @Test
        @DisplayName("실패: 비밀번호 확인 불일치")
        void failWithPasswordMismatch() throws Exception {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "verificationToken", "vp_test-token",
                                    "name", "홍길동",
                                    "userId", "dewple123",
                                    "password", "Password1!",
                                    "passwordConfirm", "DifferentPassword1!"
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(4106));
        }

        @Test
        @DisplayName("실패: 이미 가입된 전화번호")
        void failWithPhoneAlreadyExists() throws Exception {
            // given
            given(userService.signup(eq("vp_test-token"), eq("홍길동"), eq("dewple123"), eq("Password1!")))
                    .willThrow(new BusinessException(UserErrorCode.PHONE_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "verificationToken", "vp_test-token",
                                    "name", "홍길동",
                                    "userId", "dewple123",
                                    "password", "Password1!",
                                    "passwordConfirm", "Password1!"
                            ))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(4101));
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 아이디")
        void failWithUserIdAlreadyExists() throws Exception {
            // given
            given(userService.signup(eq("vp_test-token"), eq("홍길동"), eq("dewple123"), eq("Password1!")))
                    .willThrow(new BusinessException(UserErrorCode.USER_ID_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "verificationToken", "vp_test-token",
                                    "name", "홍길동",
                                    "userId", "dewple123",
                                    "password", "Password1!",
                                    "passwordConfirm", "Password1!"
                            ))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(4102));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 아이디 형식")
        void failWithInvalidUserIdFormat() throws Exception {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "verificationToken", "vp_test-token",
                                    "name", "홍길동",
                                    "userId", "ab",
                                    "password", "Password1!",
                                    "passwordConfirm", "Password1!"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 유효하지 않은 비밀번호 형식")
        void failWithInvalidPasswordFormat() throws Exception {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "verificationToken", "vp_test-token",
                                    "name", "홍길동",
                                    "userId", "dewple123",
                                    "password", "simple",
                                    "passwordConfirm", "simple"
                            ))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login - 로그인")
    class Login {

        @Test
        @DisplayName("성공: 유효한 자격증명으로 로그인")
        void success() throws Exception {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userService.login(eq("dewple123"), eq("Password1!"))).willReturn(user);
            given(jwtTokenProvider.generateAccessToken(1L)).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");
            given(jwtProperties.refreshTokenExpiry()).willReturn(604800L);

            // when & then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", "dewple123",
                                    "password", "Password1!"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(header().string("Authorization", "Bearer access-token"))
                    .andExpect(header().string("Authorization-Refresh", "Bearer refresh-token"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failWithUserNotFound() throws Exception {
            // given
            given(userService.login(eq("nonexistent"), eq("Password1!")))
                    .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", "nonexistent",
                                    "password", "Password1!"
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(4201));
        }

        @Test
        @DisplayName("실패: 비밀번호 불일치")
        void failWithPasswordMismatch() throws Exception {
            // given
            given(userService.login(eq("dewple123"), eq("WrongPassword1!")))
                    .willThrow(new BusinessException(UserErrorCode.PASSWORD_MISMATCH));

            // when & then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", "dewple123",
                                    "password", "WrongPassword1!"
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(4202));
        }

        @Test
        @DisplayName("실패: 비활성화된 계정")
        void failWithInactiveUser() throws Exception {
            // given
            given(userService.login(eq("dewple123"), eq("Password1!")))
                    .willThrow(new BusinessException(UserErrorCode.USER_INACTIVE));

            // when & then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", "dewple123",
                                    "password", "Password1!"
                            ))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(4203));
        }

        @Test
        @DisplayName("실패: 아이디 누락")
        void failWithMissingUserId() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "password", "Password1!"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 비밀번호 누락")
        void failWithMissingPassword() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", "dewple123"
                            ))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout - 로그아웃")
    class Logout {

        @Test
        @DisplayName("성공: 로그아웃하여 전체 리프레시 토큰 삭제")
        void success() throws Exception {
            // when & then
            mockMvc.perform(post("/auth/logout")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));

            verify(refreshTokenService).deleteAllByUserId(1L);
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/token/refresh - 토큰 재발급")
    class TokenRefresh {

        @Test
        @DisplayName("성공: 유효한 리프레시 토큰으로 재발급")
        void success() throws Exception {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            Jwt decodedJwt = Jwt.withTokenValue("valid-refresh-token")
                    .header("alg", "HS256")
                    .subject("1")
                    .claim("type", "refresh")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .build();

            RefreshToken storedToken = RefreshToken.builder()
                    .user(user)
                    .token("valid-refresh-token")
                    .expireAt(OffsetDateTime.now().plusDays(7))
                    .build();

            given(jwtDecoder.decode("valid-refresh-token")).willReturn(decodedJwt);
            given(refreshTokenService.findByToken("valid-refresh-token")).willReturn(Optional.of(storedToken));
            given(userService.findById(1L)).willReturn(user);
            given(jwtTokenProvider.generateAccessToken(1L)).willReturn("new-access-token");
            given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("new-refresh-token");
            given(jwtProperties.refreshTokenExpiry()).willReturn(604800L);

            // when & then
            mockMvc.perform(post("/auth/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "refreshToken", "valid-refresh-token"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(header().string("Authorization", "Bearer new-access-token"))
                    .andExpect(header().string("Authorization-Refresh", "Bearer new-refresh-token"));

            verify(refreshTokenService).deleteByToken("valid-refresh-token");
        }

        @Test
        @DisplayName("실패: 만료된 리프레시 토큰")
        void failWithExpiredToken() throws Exception {
            // given
            given(jwtDecoder.decode("expired-token"))
                    .willThrow(new JwtException("Token expired"));

            // when & then
            mockMvc.perform(post("/auth/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "refreshToken", "expired-token"
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(4302));
        }

        @Test
        @DisplayName("실패: 액세스 토큰으로 재발급 시도")
        void failWithAccessToken() throws Exception {
            // given
            Jwt decodedJwt = Jwt.withTokenValue("access-token")
                    .header("alg", "HS256")
                    .subject("1")
                    .claim("type", "access")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            given(jwtDecoder.decode("access-token")).willReturn(decodedJwt);

            // when & then
            mockMvc.perform(post("/auth/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "refreshToken", "access-token"
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(4301));
        }

        @Test
        @DisplayName("실패: 이미 사용된 리프레시 토큰 (DB에 없음)")
        void failWithAlreadyUsedToken() throws Exception {
            // given
            Jwt decodedJwt = Jwt.withTokenValue("used-token")
                    .header("alg", "HS256")
                    .subject("1")
                    .claim("type", "refresh")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .build();

            given(jwtDecoder.decode("used-token")).willReturn(decodedJwt);
            given(refreshTokenService.findByToken("used-token")).willReturn(Optional.empty());

            // when & then
            mockMvc.perform(post("/auth/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "refreshToken", "used-token"
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(4301));
        }

        @Test
        @DisplayName("실패: 리프레시 토큰 누락")
        void failWithMissingToken() throws Exception {
            mockMvc.perform(post("/auth/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
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

    private Verification createEmailVerification() {
        Verification verification = Verification.builder()
                .type(VerificationType.EMAIL)
                .target("user@example.com")
                .code("A1B2C3")
                .purpose(VerificationPurpose.SIGN_UP)
                .build();
        ReflectionTestUtils.setField(verification, "createdAt", OffsetDateTime.now());
        return verification;
    }

    private User createUser() {
        return User.builder()
                .userId("dewple123")
                .password("encoded-password")
                .name("홍길동")
                .phone("01012345678")
                .build();
    }
}
