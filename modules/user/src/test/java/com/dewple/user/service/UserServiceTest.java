package com.dewple.user.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.University;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.port.PasswordEncoderPort;
import com.dewple.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationService verificationService;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private UserService userService;

    private static final String TEST_PHONE = "01012345678";
    private static final String TEST_USER_ID = "dewple123";
    private static final String TEST_PASSWORD = "Password1!";
    private static final String TEST_NAME = "홍길동";
    private static final String TEST_TOKEN = "vp_test-token";

    @Nested
    @DisplayName("signup - 회원가입")
    class Signup {

        @Test
        @DisplayName("성공: 유효한 정보로 회원가입")
        void success() {
            // given
            given(verificationService.validateVerificationToken(TEST_TOKEN)).willReturn(TEST_PHONE);
            given(userRepository.existsByPhone(TEST_PHONE)).willReturn(false);
            given(userRepository.existsByUserId(TEST_USER_ID)).willReturn(false);
            given(passwordEncoderPort.encode(TEST_PASSWORD)).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User result = userService.signup(TEST_TOKEN, TEST_NAME, TEST_USER_ID, TEST_PASSWORD);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getName()).isEqualTo(TEST_NAME);
            assertThat(result.getPhone()).isEqualTo(TEST_PHONE);
            assertThat(result.getPassword()).isEqualTo("encoded-password");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("실패: 이미 가입된 전화번호")
        void failWithPhoneAlreadyExists() {
            // given
            given(verificationService.validateVerificationToken(TEST_TOKEN)).willReturn(TEST_PHONE);
            given(userRepository.existsByPhone(TEST_PHONE)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(TEST_TOKEN, TEST_NAME, TEST_USER_ID, TEST_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.PHONE_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 아이디")
        void failWithUserIdAlreadyExists() {
            // given
            given(verificationService.validateVerificationToken(TEST_TOKEN)).willReturn(TEST_PHONE);
            given(userRepository.existsByPhone(TEST_PHONE)).willReturn(false);
            given(userRepository.existsByUserId(TEST_USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(TEST_TOKEN, TEST_NAME, TEST_USER_ID, TEST_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_ID_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("실패: 유효하지 않은 본인인증 토큰")
        void failWithInvalidToken() {
            // given
            given(verificationService.validateVerificationToken("invalid-token"))
                    .willThrow(new BusinessException(UserErrorCode.VERIFICATION_TOKEN_INVALID));

            // when & then
            assertThatThrownBy(() -> userService.signup("invalid-token", TEST_NAME, TEST_USER_ID, TEST_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.VERIFICATION_TOKEN_INVALID);
                    });
        }
    }

    @Nested
    @DisplayName("isUserIdAvailable - 아이디 중복 확인")
    class IsUserIdAvailable {

        @Test
        @DisplayName("성공: 사용 가능한 아이디")
        void available() {
            // given
            given(userRepository.existsByUserId(TEST_USER_ID)).willReturn(false);

            // when
            boolean result = userService.isUserIdAvailable(TEST_USER_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공: 이미 사용 중인 아이디")
        void notAvailable() {
            // given
            given(userRepository.existsByUserId(TEST_USER_ID)).willReturn(true);

            // when
            boolean result = userService.isUserIdAvailable(TEST_USER_ID);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("updateProfile - 프로필 업데이트")
    class UpdateProfile {

        @Test
        @DisplayName("성공: 프로필 업데이트")
        void success() {
            // given
            User user = createUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("듀플러")).willReturn(false);
            given(userRepository.existsByEmail("test@example.com")).willReturn(false);

            // when
            User result = userService.updateProfile(1L, "듀플러", "test@example.com",
                    LocalDate.of(2000, 1, 1), Gender.MALE, University.SEOUL_NATIONAL, false, "듀플");

            // then
            assertThat(result.getNickname()).isEqualTo("듀플러");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getBirthdate()).isEqualTo(LocalDate.of(2000, 1, 1));
            assertThat(result.getGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 닉네임")
        void failWithNicknameAlreadyExists() {
            // given
            User user = createUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("듀플러")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(1L, "듀플러", null,
                    null, null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.NICKNAME_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 이메일")
        void failWithEmailAlreadyExists() {
            // given
            User user = createUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByEmail("test@example.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(1L, null, "test@example.com",
                    null, null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failWithUserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(999L, null, null,
                    null, null, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("login - 로그인")
    class Login {

        @Test
        @DisplayName("성공: 유효한 자격증명으로 로그인")
        void success() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findByUserId(TEST_USER_ID)).willReturn(Optional.of(user));
            given(passwordEncoderPort.matches(TEST_PASSWORD, "encoded-password")).willReturn(true);

            // when
            User result = userService.login(TEST_USER_ID, TEST_PASSWORD);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failWithUserNotFound() {
            // given
            given(userRepository.findByUserId("nonexistent")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login("nonexistent", TEST_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 비밀번호 불일치")
        void failWithPasswordMismatch() {
            // given
            User user = createUser();
            given(userRepository.findByUserId(TEST_USER_ID)).willReturn(Optional.of(user));
            given(passwordEncoderPort.matches("wrong-password", "encoded-password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(TEST_USER_ID, "wrong-password"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.PASSWORD_MISMATCH);
                    });
        }

        @Test
        @DisplayName("실패: 비활성화된 계정")
        void failWithInactiveUser() {
            // given
            User user = createUser();
            user.inactivate();
            given(userRepository.findByUserId(TEST_USER_ID)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.login(TEST_USER_ID, TEST_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_INACTIVE);
                    });
        }
    }

    private User createUser() {
        return User.builder()
                .userId(TEST_USER_ID)
                .password("encoded-password")
                .name(TEST_NAME)
                .phone(TEST_PHONE)
                .build();
    }
}
