package com.dewple.app_api_auth.api.activity.controller;

import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.service.ActivityService;
import com.dewple.activity.service.CreateActivityResult;
import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.common.enums.OpenType;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
@Import(SecurityConfig.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String START_AT = "2026-04-01T10:00:00+09:00";
    private static final String END_AT = "2026-04-01T12:00:00+09:00";

    @Nested
    @DisplayName("POST /activities - 모임 생성")
    class CreateActivity {

        @Test
        @DisplayName("성공: 개인 모임 생성")
        void successWithPersonalActivity() throws Exception {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            CreateActivityResult result = new CreateActivityResult(
                    100L, null, null, OpenType.PUBLIC,
                    "봄맞이 독서 모임", "함께 책을 읽어요", 20,
                    false, true,
                    OffsetDateTime.parse(START_AT), OffsetDateTime.parse(END_AT), now
            );

            given(activityService.createActivity(eq(1L), any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", "봄맞이 독서 모임",
                                    "description", "함께 책을 읽어요",
                                    "capacity", 20,
                                    "isAttendanceCheck", false,
                                    "isSearchable", true,
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.activityId").value(100))
                    .andExpect(jsonPath("$.result.clubId").doesNotExist())
                    .andExpect(jsonPath("$.result.clubName").doesNotExist())
                    .andExpect(jsonPath("$.result.openType").value("PUBLIC"))
                    .andExpect(jsonPath("$.result.name").value("봄맞이 독서 모임"))
                    .andExpect(jsonPath("$.result.description").value("함께 책을 읽어요"))
                    .andExpect(jsonPath("$.result.capacity").value(20))
                    .andExpect(jsonPath("$.result.isAttendanceCheck").value(false))
                    .andExpect(jsonPath("$.result.isSearchable").value(true));
        }

        @Test
        @DisplayName("성공: 동아리 모임 생성")
        void successWithClubActivity() throws Exception {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            CreateActivityResult result = new CreateActivityResult(
                    101L, 10L, "테스트 동아리", OpenType.PRIVATE,
                    "정기 모임", "이번 주 정기 모임", null,
                    true, false,
                    OffsetDateTime.parse(START_AT), OffsetDateTime.parse(END_AT), now
            );

            given(activityService.createActivity(eq(1L), any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "clubId", 10,
                                    "openType", "PRIVATE",
                                    "name", "정기 모임",
                                    "description", "이번 주 정기 모임",
                                    "isAttendanceCheck", true,
                                    "isSearchable", false,
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.activityId").value(101))
                    .andExpect(jsonPath("$.result.clubId").value(10))
                    .andExpect(jsonPath("$.result.clubName").value("테스트 동아리"))
                    .andExpect(jsonPath("$.result.openType").value("PRIVATE"))
                    .andExpect(jsonPath("$.result.isAttendanceCheck").value(true))
                    .andExpect(jsonPath("$.result.isSearchable").value(false));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(post("/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 모임 이름 누락")
        void failWithMissingName() throws Exception {
            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "description", "설명",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 모임 설명 누락")
        void failWithMissingDescription() throws Exception {
            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 공개 타입 누락")
        void failWithMissingOpenType() throws Exception {
            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "모임",
                                    "description", "설명",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 시작 시간 누락")
        void failWithMissingStartAt() throws Exception {
            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 종료 시간 누락")
        void failWithMissingEndAt() throws Exception {
            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "startAt", START_AT
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: capacity가 0 이하")
        void failWithInvalidCapacity() throws Exception {
            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "capacity", 0,
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 모임 이름이 100자 초과")
        void failWithNameTooLong() throws Exception {
            // when & then
            String longName = "가".repeat(101);
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", longName,
                                    "description", "설명",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 종료 시간이 시작 시간보다 이전 (서비스 예외)")
        void failWithEndBeforeStart() throws Exception {
            // given
            given(activityService.createActivity(eq(1L), any()))
                    .willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_END_BEFORE_START));

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "startAt", END_AT,
                                    "endAt", START_AT
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5001));
        }

        @Test
        @DisplayName("실패: 동아리 권한 없음 (서비스 예외)")
        void failWithClubPermissionDenied() throws Exception {
            // given
            given(activityService.createActivity(eq(1L), any()))
                    .willThrow(new BusinessException(ClubErrorCode.CLUB_PERMISSION_DENIED));

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "clubId", 10,
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(6002));
        }

        @Test
        @DisplayName("실패: 동아리 멤버가 아님 (서비스 예외)")
        void failWithNotClubMember() throws Exception {
            // given
            given(activityService.createActivity(eq(1L), any()))
                    .willThrow(new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "clubId", 10,
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(6001));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 동아리 (서비스 예외)")
        void failWithClubNotFound() throws Exception {
            // given
            given(activityService.createActivity(eq(1L), any()))
                    .willThrow(new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "clubId", 999,
                                    "openType", "PUBLIC",
                                    "name", "모임",
                                    "description", "설명",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(6000));
        }
    }
}
