package com.dewple.app_api_auth.api.user.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.entity.User;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.Mbti;
import com.dewple.common.enums.University;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.service.EditMyProfileParam;
import com.dewple.user.service.MyProfileResult;
import com.dewple.user.service.UpdateProfileParam;
import com.dewple.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("GET /users/me - 내 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공: 모든 필드가 채워진 프로필 조회")
        void successWithFullProfile() throws Exception {
            // given
            User user = User.builder()
                    .userId("dewple123")
                    .password("encoded-password")
                    .name("홍길동")
                    .phone("01012345678")
                    .nickname("듀플러")
                    .email("test@example.com")
                    .birthdate(LocalDate.of(2000, 1, 1))
                    .gender(Gender.MALE)
                    .university(University.SEOUL_NATIONAL)
                    .isGraduated(false)
                    .workplace("듀플")
                    .selfIntroduction("안녕하세요")
                    .mbti(Mbti.INTJ)
                    .profileImg("https://example.com/img.jpg")
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userService.getMyProfile(1L))
                    .willReturn(new MyProfileResult(user, List.of("개발", "디자인")));

            // when & then
            mockMvc.perform(get("/users/me")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.name").value("홍길동"))
                    .andExpect(jsonPath("$.result.profileImg").value("https://example.com/img.jpg"))
                    .andExpect(jsonPath("$.result.nickname").value("듀플러"))
                    .andExpect(jsonPath("$.result.email").value("test@example.com"))
                    .andExpect(jsonPath("$.result.phone").value("01012345678"))
                    .andExpect(jsonPath("$.result.birthdate").value("2000-01-01"))
                    .andExpect(jsonPath("$.result.gender").value("MALE"))
                    .andExpect(jsonPath("$.result.university").value("SEOUL_NATIONAL"))
                    .andExpect(jsonPath("$.result.isGraduated").value(false))
                    .andExpect(jsonPath("$.result.workplace").value("듀플"))
                    .andExpect(jsonPath("$.result.selfIntroduction").value("안녕하세요"))
                    .andExpect(jsonPath("$.result.mbti").value("INTJ"))
                    .andExpect(jsonPath("$.result.interests").isArray())
                    .andExpect(jsonPath("$.result.interests.length()").value(2))
                    .andExpect(jsonPath("$.result.interests[0]").value("개발"))
                    .andExpect(jsonPath("$.result.interests[1]").value("디자인"));
        }

        @Test
        @DisplayName("성공: 선택 필드가 null인 프로필 조회 (회원가입 직후)")
        void successWithMinimalProfile() throws Exception {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userService.getMyProfile(1L))
                    .willReturn(new MyProfileResult(user, Collections.emptyList()));

            // when & then
            mockMvc.perform(get("/users/me")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.name").value("홍길동"))
                    .andExpect(jsonPath("$.result.phone").value("01012345678"))
                    .andExpect(jsonPath("$.result.profileImg").doesNotExist())
                    .andExpect(jsonPath("$.result.nickname").doesNotExist())
                    .andExpect(jsonPath("$.result.email").doesNotExist())
                    .andExpect(jsonPath("$.result.birthdate").doesNotExist())
                    .andExpect(jsonPath("$.result.gender").doesNotExist())
                    .andExpect(jsonPath("$.result.university").doesNotExist())
                    .andExpect(jsonPath("$.result.isGraduated").doesNotExist())
                    .andExpect(jsonPath("$.result.workplace").doesNotExist())
                    .andExpect(jsonPath("$.result.selfIntroduction").doesNotExist())
                    .andExpect(jsonPath("$.result.mbti").doesNotExist())
                    .andExpect(jsonPath("$.result.interests").isArray())
                    .andExpect(jsonPath("$.result.interests").isEmpty());
        }

        @Test
        @DisplayName("성공: 관심 분야가 없는 사용자")
        void successWithNoInterests() throws Exception {
            // given
            User user = User.builder()
                    .userId("dewple123")
                    .password("encoded-password")
                    .name("홍길동")
                    .phone("01012345678")
                    .nickname("듀플러")
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userService.getMyProfile(1L))
                    .willReturn(new MyProfileResult(user, Collections.emptyList()));

            // when & then
            mockMvc.perform(get("/users/me")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.nickname").value("듀플러"))
                    .andExpect(jsonPath("$.result.interests").isArray())
                    .andExpect(jsonPath("$.result.interests").isEmpty());
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuth() throws Exception {
            // when & then
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failWithUserNotFound() throws Exception {
            // given
            given(userService.getMyProfile(999L))
                    .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/users/me")
                            .with(jwt().jwt(j -> j.subject("999"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(4201));
        }
    }

    @Nested
    @DisplayName("GET /users/check-userid - 아이디 중복 확인")
    class CheckUserId {

        @Test
        @DisplayName("성공: 사용 가능한 아이디")
        void available() throws Exception {
            // given
            given(userService.isUserIdAvailable("dewple123")).willReturn(true);

            // when & then
            mockMvc.perform(get("/users/check-userid")
                            .param("userId", "dewple123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.isAvailable").value(true));
        }

        @Test
        @DisplayName("성공: 이미 사용 중인 아이디")
        void notAvailable() throws Exception {
            // given
            given(userService.isUserIdAvailable("existing")).willReturn(false);

            // when & then
            mockMvc.perform(get("/users/check-userid")
                            .param("userId", "existing"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.isAvailable").value(false));
        }
    }

    @Nested
    @DisplayName("PATCH /users/me/profile - 프로필 설정")
    class UpdateProfile {

        @Test
        @DisplayName("성공: 프로필 업데이트")
        void success() throws Exception {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.updateProfile("듀플러", "test@example.com",
                    LocalDate.of(2000, 1, 1), Gender.MALE,
                    University.SEOUL_NATIONAL, false, "듀플");

            given(userService.updateProfile(eq(1L), any(UpdateProfileParam.class)))
                    .willReturn(user);

            // when & then
            mockMvc.perform(patch("/users/me/profile")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nickname", "듀플러",
                                    "email", "test@example.com",
                                    "birthdate", "2000-01-01",
                                    "gender", "MALE",
                                    "university", "SEOUL_NATIONAL",
                                    "isGraduated", false,
                                    "workplace", "듀플"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.nickname").value("듀플러"))
                    .andExpect(jsonPath("$.result.email").value("test@example.com"));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(patch("/users/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nickname", "듀플러"
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 닉네임")
        void failWithNicknameAlreadyExists() throws Exception {
            // given
            given(userService.updateProfile(eq(1L), any(UpdateProfileParam.class)))
                    .willThrow(new BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(patch("/users/me/profile")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"듀플러\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(4103));
        }
    }

    @Nested
    @DisplayName("PATCH /users/me - 프로필 수정")
    class EditMyProfile {

        @Test
        @DisplayName("성공: 닉네임만 수정 + 전체 프로필 응답 확인")
        void successWithNicknameOnly() throws Exception {
            // given
            User user = User.builder()
                    .userId("dewple123")
                    .password("encoded-password")
                    .name("홍길동")
                    .phone("01012345678")
                    .nickname("새닉네임")
                    .email("test@example.com")
                    .birthdate(LocalDate.of(2000, 1, 1))
                    .gender(Gender.MALE)
                    .university(University.SEOUL_NATIONAL)
                    .isGraduated(false)
                    .workplace("듀플")
                    .selfIntroduction("안녕하세요")
                    .mbti(Mbti.INTJ)
                    .profileImg("https://example.com/img.jpg")
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userService.editMyProfile(eq(1L), any(EditMyProfileParam.class)))
                    .willReturn(new MyProfileResult(user, List.of("개발")));

            // when & then
            mockMvc.perform(patch("/users/me")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"새닉네임\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.name").value("홍길동"))
                    .andExpect(jsonPath("$.result.nickname").value("새닉네임"))
                    .andExpect(jsonPath("$.result.email").value("test@example.com"))
                    .andExpect(jsonPath("$.result.phone").value("01012345678"))
                    .andExpect(jsonPath("$.result.selfIntroduction").value("안녕하세요"))
                    .andExpect(jsonPath("$.result.mbti").value("INTJ"))
                    .andExpect(jsonPath("$.result.interests[0]").value("개발"));
        }

        @Test
        @DisplayName("성공: categoryIds 포함 수정")
        void successWithCategoryIds() throws Exception {
            // given
            User user = createUser();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userService.editMyProfile(eq(1L), any(EditMyProfileParam.class)))
                    .willReturn(new MyProfileResult(user, List.of("개발", "디자인")));

            // when & then
            mockMvc.perform(patch("/users/me")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"categoryIds\":[1,2]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.interests.length()").value(2))
                    .andExpect(jsonPath("$.result.interests[0]").value("개발"))
                    .andExpect(jsonPath("$.result.interests[1]").value("디자인"));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(patch("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"새닉네임\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 닉네임 중복")
        void failWithNicknameAlreadyExists() throws Exception {
            // given
            given(userService.editMyProfile(eq(1L), any(EditMyProfileParam.class)))
                    .willThrow(new BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(patch("/users/me")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"중복닉네임\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(4103));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리")
        void failWithCategoryNotFound() throws Exception {
            // given
            given(userService.editMyProfile(eq(1L), any(EditMyProfileParam.class)))
                    .willThrow(new BusinessException(UserErrorCode.CATEGORY_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/users/me")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"categoryIds\":[999]}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(4400));
        }

        @Test
        @DisplayName("실패: 닉네임 유효성 검증 (1글자)")
        void failWithNicknameValidation() throws Exception {
            mockMvc.perform(patch("/users/me")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"A\"}"))
                    .andExpect(status().isBadRequest());
        }
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
