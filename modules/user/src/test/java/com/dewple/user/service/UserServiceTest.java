package com.dewple.user.service;

import com.dewple.common.entity.Category;
import com.dewple.common.entity.User;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.Mbti;
import com.dewple.common.enums.University;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.UserCategory;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.port.PasswordEncoderPort;
import com.dewple.user.repository.CategoryRepository;
import com.dewple.user.repository.UserCategoryRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCategoryRepository userCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

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
            User result = userService.signup(new SignupParam(TEST_TOKEN, TEST_NAME, TEST_USER_ID, TEST_PASSWORD));

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
            assertThatThrownBy(() -> userService.signup(new SignupParam(TEST_TOKEN, TEST_NAME, TEST_USER_ID, TEST_PASSWORD)))
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
            assertThatThrownBy(() -> userService.signup(new SignupParam(TEST_TOKEN, TEST_NAME, TEST_USER_ID, TEST_PASSWORD)))
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
            assertThatThrownBy(() -> userService.signup(new SignupParam("invalid-token", TEST_NAME, TEST_USER_ID, TEST_PASSWORD)))
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
            User result = userService.updateProfile(1L, new UpdateProfileParam(
                    "듀플러", "test@example.com",
                    LocalDate.of(2000, 1, 1), Gender.MALE, University.SEOUL_NATIONAL, false, "듀플"));

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
            assertThatThrownBy(() -> userService.updateProfile(1L, new UpdateProfileParam(
                    "듀플러", null, null, null, null, null, null)))
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
            assertThatThrownBy(() -> userService.updateProfile(1L, new UpdateProfileParam(
                    null, "test@example.com", null, null, null, null, null)))
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
            assertThatThrownBy(() -> userService.updateProfile(999L, new UpdateProfileParam(
                    null, null, null, null, null, null, null)))
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
            User result = userService.login(new LoginParam(TEST_USER_ID, TEST_PASSWORD));

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
            assertThatThrownBy(() -> userService.login(new LoginParam("nonexistent", TEST_PASSWORD)))
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
            assertThatThrownBy(() -> userService.login(new LoginParam(TEST_USER_ID, "wrong-password")))
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
            assertThatThrownBy(() -> userService.login(new LoginParam(TEST_USER_ID, TEST_PASSWORD)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_INACTIVE);
                    });
        }
    }

    @Nested
    @DisplayName("getMyProfile - 내 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공: 모든 필드가 채워진 사용자 + 관심 분야 포함")
        void successWithFullProfile() {
            // given
            User user = User.builder()
                    .userId(TEST_USER_ID)
                    .password("encoded-password")
                    .name(TEST_NAME)
                    .phone(TEST_PHONE)
                    .nickname("듀플러")
                    .email("test@example.com")
                    .birthdate(LocalDate.of(2000, 1, 1))
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);

            Category category1 = Category.builder().name("개발").build();
            Category category2 = Category.builder().name("디자인").build();
            UserCategory uc1 = UserCategory.builder().user(user).category(category1).build();
            UserCategory uc2 = UserCategory.builder().user(user).category(category2).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(List.of(uc1, uc2));

            // when
            MyProfileResult result = userService.getMyProfile(1L);

            // then
            assertThat(result.user().getName()).isEqualTo(TEST_NAME);
            assertThat(result.user().getNickname()).isEqualTo("듀플러");
            assertThat(result.user().getEmail()).isEqualTo("test@example.com");
            assertThat(result.user().getPhone()).isEqualTo(TEST_PHONE);
            assertThat(result.user().getBirthdate()).isEqualTo(LocalDate.of(2000, 1, 1));
            assertThat(result.interests()).containsExactly("개발", "디자인");
        }

        @Test
        @DisplayName("성공: 선택 필드가 null인 사용자 (회원가입 직후)")
        void successWithMinimalProfile() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            // when
            MyProfileResult result = userService.getMyProfile(1L);

            // then
            assertThat(result.user().getName()).isEqualTo(TEST_NAME);
            assertThat(result.user().getPhone()).isEqualTo(TEST_PHONE);
            assertThat(result.user().getNickname()).isNull();
            assertThat(result.user().getEmail()).isNull();
            assertThat(result.user().getBirthdate()).isNull();
            assertThat(result.user().getGender()).isNull();
            assertThat(result.user().getUniversity()).isNull();
            assertThat(result.user().getIsGraduated()).isNull();
            assertThat(result.user().getWorkplace()).isNull();
            assertThat(result.user().getSelfIntroduction()).isNull();
            assertThat(result.user().getMbti()).isNull();
            assertThat(result.user().getProfileImg()).isNull();
            assertThat(result.interests()).isEmpty();
        }

        @Test
        @DisplayName("성공: 관심 분야가 없는 사용자")
        void successWithNoInterests() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            // when
            MyProfileResult result = userService.getMyProfile(1L);

            // then
            assertThat(result.interests()).isEmpty();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failWithUserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyProfile(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("getUserProfile - 회원 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("성공: 모든 공개 필드가 채워진 회원")
        void successWithFullProfile() {
            // given
            User user = User.builder()
                    .userId(TEST_USER_ID)
                    .password("encoded-password")
                    .name(TEST_NAME)
                    .phone(TEST_PHONE)
                    .profileImg("https://example.com/img.jpg")
                    .selfIntroduction("안녕하세요")
                    .mbti(Mbti.INTJ)
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);

            Category category1 = Category.builder().name("개발").build();
            Category category2 = Category.builder().name("디자인").build();
            UserCategory uc1 = UserCategory.builder().user(user).category(category1).build();
            UserCategory uc2 = UserCategory.builder().user(user).category(category2).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(List.of(uc1, uc2));

            // when
            UserProfileResult result = userService.getUserProfile(1L);

            // then
            assertThat(result.name()).isEqualTo(TEST_NAME);
            assertThat(result.profileImg()).isEqualTo("https://example.com/img.jpg");
            assertThat(result.selfIntroduction()).isEqualTo("안녕하세요");
            assertThat(result.mbti()).isEqualTo("INTJ");
            assertThat(result.interests()).containsExactly("개발", "디자인");
        }

        @Test
        @DisplayName("성공: 선택 필드가 모두 null인 회원 (회원가입 직후)")
        void successWithMinimalProfile() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            // when
            UserProfileResult result = userService.getUserProfile(1L);

            // then
            assertThat(result.name()).isEqualTo(TEST_NAME);
            assertThat(result.profileImg()).isNull();
            assertThat(result.selfIntroduction()).isNull();
            assertThat(result.mbti()).isNull();
            assertThat(result.interests()).isEmpty();
        }

        @Test
        @DisplayName("성공: 관심 분야만 없는 회원")
        void successWithNoInterests() {
            // given
            User user = User.builder()
                    .userId(TEST_USER_ID)
                    .password("encoded-password")
                    .name(TEST_NAME)
                    .phone(TEST_PHONE)
                    .selfIntroduction("자기소개입니다")
                    .mbti(Mbti.ENFP)
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            // when
            UserProfileResult result = userService.getUserProfile(1L);

            // then
            assertThat(result.selfIntroduction()).isEqualTo("자기소개입니다");
            assertThat(result.mbti()).isEqualTo("ENFP");
            assertThat(result.interests()).isEmpty();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void failWithUserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("editMyProfile - 프로필 수정")
    class EditMyProfile {

        @Test
        @DisplayName("성공: 닉네임만 수정 (partial)")
        void successWithNicknameOnly() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.updateProfile("기존닉네임", "old@example.com", LocalDate.of(1999, 1, 1),
                    Gender.MALE, University.SEOUL_NATIONAL, false, "기존직장");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L)).willReturn(false);
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            EditMyProfileParam command = new EditMyProfileParam(
                    "새닉네임", null, null, null, null, null, null, null, null, null, null);

            // when
            MyProfileResult result = userService.editMyProfile(1L, command);

            // then
            assertThat(result.user().getNickname()).isEqualTo("새닉네임");
            assertThat(result.user().getEmail()).isEqualTo("old@example.com");
            assertThat(result.user().getGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("성공: 여러 필드 동시 수정")
        void successWithMultipleFields() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L)).willReturn(false);
            given(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).willReturn(false);
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            EditMyProfileParam command = new EditMyProfileParam(
                    "새닉네임", "new@example.com", null, null, null, null, null, null, null, Mbti.INTJ, null);

            // when
            MyProfileResult result = userService.editMyProfile(1L, command);

            // then
            assertThat(result.user().getNickname()).isEqualTo("새닉네임");
            assertThat(result.user().getEmail()).isEqualTo("new@example.com");
            assertThat(result.user().getMbti()).isEqualTo(Mbti.INTJ);
        }

        @Test
        @DisplayName("성공: 관심분야 교체")
        void successWithCategoryReplacement() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            Category cat1 = Category.builder().name("개발").build();
            ReflectionTestUtils.setField(cat1, "id", 1L);
            Category cat2 = Category.builder().name("디자인").build();
            ReflectionTestUtils.setField(cat2, "id", 2L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(categoryRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(cat1, cat2));
            given(userCategoryRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

            UserCategory uc1 = UserCategory.builder().user(user).category(cat1).build();
            UserCategory uc2 = UserCategory.builder().user(user).category(cat2).build();
            given(userCategoryRepository.findByUserId(1L)).willReturn(List.of(uc1, uc2));

            EditMyProfileParam command = new EditMyProfileParam(
                    null, null, null, null, null, null, null, null, null, null, List.of(1L, 2L));

            // when
            MyProfileResult result = userService.editMyProfile(1L, command);

            // then
            verify(userCategoryRepository).deleteByUserId(1L);
            verify(userCategoryRepository).saveAll(anyList());
            assertThat(result.interests()).containsExactly("개발", "디자인");
        }

        @Test
        @DisplayName("성공: 관심분야 전체 삭제")
        void successWithCategoryDeletion() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            EditMyProfileParam command = new EditMyProfileParam(
                    null, null, null, null, null, null, null, null, null, null, List.of());

            // when
            MyProfileResult result = userService.editMyProfile(1L, command);

            // then
            verify(userCategoryRepository).deleteByUserId(1L);
            verify(userCategoryRepository, never()).saveAll(anyList());
            assertThat(result.interests()).isEmpty();
        }

        @Test
        @DisplayName("성공: 관심분야 미변경 (categoryIds가 null)")
        void successWithCategoryUnchanged() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            EditMyProfileParam command = new EditMyProfileParam(
                    null, null, null, null, null, null, null, null, null, null, null);

            // when
            userService.editMyProfile(1L, command);

            // then
            verify(userCategoryRepository, never()).deleteByUserId(anyLong());
        }

        @Test
        @DisplayName("성공: 자기 닉네임 유지 시 중복 검사 통과")
        void successWithOwnNickname() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.updateProfile("기존닉네임", null, null, null, null, null, null);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("기존닉네임", 1L)).willReturn(false);
            given(userCategoryRepository.findByUserId(1L)).willReturn(Collections.emptyList());

            EditMyProfileParam command = new EditMyProfileParam(
                    "기존닉네임", null, null, null, null, null, null, null, null, null, null);

            // when
            MyProfileResult result = userService.editMyProfile(1L, command);

            // then
            assertThat(result.user().getNickname()).isEqualTo("기존닉네임");
        }

        @Test
        @DisplayName("실패: 사용자 미존재")
        void failWithUserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            EditMyProfileParam command = new EditMyProfileParam(
                    "닉네임", null, null, null, null, null, null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> userService.editMyProfile(999L, command))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 닉네임 중복")
        void failWithNicknameAlreadyExists() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByNicknameAndIdNot("중복닉네임", 1L)).willReturn(true);

            EditMyProfileParam command = new EditMyProfileParam(
                    "중복닉네임", null, null, null, null, null, null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> userService.editMyProfile(1L, command))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.NICKNAME_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("실패: 이메일 중복")
        void failWithEmailAlreadyExists() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.existsByEmailAndIdNot("dup@example.com", 1L)).willReturn(true);

            EditMyProfileParam command = new EditMyProfileParam(
                    null, "dup@example.com", null, null, null, null, null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> userService.editMyProfile(1L, command))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리")
        void failWithCategoryNotFound() {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(categoryRepository.findAllById(List.of(1L, 999L))).willReturn(
                    List.of(Category.builder().name("개발").build()));

            EditMyProfileParam command = new EditMyProfileParam(
                    null, null, null, null, null, null, null, null, null, null, List.of(1L, 999L));

            // when & then
            assertThatThrownBy(() -> userService.editMyProfile(1L, command))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.CATEGORY_NOT_FOUND);
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
