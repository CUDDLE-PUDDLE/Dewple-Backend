package com.dewple.app_api_auth.api.activity.controller;

import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.service.ActivityService;
import com.dewple.activity.service.ActivitySummaryResult;
import com.dewple.activity.service.CreateActivityResult;
import com.dewple.activity.service.GetActivityDetailResult;
import com.dewple.activity.service.ParticipantResult;
import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.OpenType;
import com.dewple.common.enums.ParticipantStatus;
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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @DisplayName("GET /activities - 모임 목록 조회")
    class GetActivityList {

        @Test
        @DisplayName("성공: PERSONAL 섹션 조회")
        void successWithPersonalSection() throws Exception {
            // given
            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(1L, "thumb1.jpg", "PERSONAL", null, "개인 모임",
                            "카테고리1", "서울", 5, 20, 10, 100, 3, false),
                    new ActivitySummaryResult(2L, null, "PERSONAL", null, "개인 모임2",
                            null, null, 0, null, 0, 0, 0, true)
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            given(activityService.getActivityList(eq(1L), any())).willReturn(slice);

            // when & then
            mockMvc.perform(get("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("section", "PERSONAL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content.length()").value(2))
                    .andExpect(jsonPath("$.result.content[0].activityId").value(1))
                    .andExpect(jsonPath("$.result.content[0].thumbnailUrl").value("thumb1.jpg"))
                    .andExpect(jsonPath("$.result.content[0].activityType").value("PERSONAL"))
                    .andExpect(jsonPath("$.result.content[0].name").value("개인 모임"))
                    .andExpect(jsonPath("$.result.content[0].categoryName").value("카테고리1"))
                    .andExpect(jsonPath("$.result.content[0].regionName").value("서울"))
                    .andExpect(jsonPath("$.result.content[0].participantCount").value(5))
                    .andExpect(jsonPath("$.result.content[0].capacity").value(20))
                    .andExpect(jsonPath("$.result.content[0].likeCount").value(10))
                    .andExpect(jsonPath("$.result.content[0].viewCount").value(100))
                    .andExpect(jsonPath("$.result.content[0].commentCount").value(3))
                    .andExpect(jsonPath("$.result.content[0].isLiked").value(false))
                    .andExpect(jsonPath("$.result.page").value(0))
                    .andExpect(jsonPath("$.result.size").value(10))
                    .andExpect(jsonPath("$.result.hasNext").value(false));
        }

        @Test
        @DisplayName("성공: LIKED_CLUBS 섹션 조회")
        void successWithLikedClubsSection() throws Exception {
            // given
            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(3L, "thumb3.jpg", "CLUB", "관심 동아리", "동아리 모임",
                            "운동", "부산", 10, 30, 5, 50, 1, true)
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), true);

            given(activityService.getActivityList(eq(1L), any())).willReturn(slice);

            // when & then
            mockMvc.perform(get("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("section", "LIKED_CLUBS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content[0].activityType").value("CLUB"))
                    .andExpect(jsonPath("$.result.content[0].clubName").value("관심 동아리"))
                    .andExpect(jsonPath("$.result.hasNext").value(true));
        }

        @Test
        @DisplayName("성공: MY_CLUBS 섹션 조회")
        void successWithMyClubsSection() throws Exception {
            // given
            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(4L, null, "CLUB", "내 동아리", "정기 모임",
                            "스터디", "서울", 8, 15, 3, 20, 0, false)
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            given(activityService.getActivityList(eq(1L), any())).willReturn(slice);

            // when & then
            mockMvc.perform(get("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("section", "MY_CLUBS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content[0].clubName").value("내 동아리"))
                    .andExpect(jsonPath("$.result.content[0].name").value("정기 모임"));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(get("/activities")
                            .param("section", "PERSONAL"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: section 파라미터 누락")
        void failWithMissingSection() throws Exception {
            // when & then
            mockMvc.perform(get("/activities")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패: 잘못된 section 값")
        void failWithInvalidSection() throws Exception {
            // when & then
            mockMvc.perform(get("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("section", "INVALID"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 (서비스 예외)")
        void failWithUserNotFound() throws Exception {
            // given
            given(activityService.getActivityList(eq(1L), any()))
                    .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("section", "PERSONAL"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(4201));
        }
    }

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
                    OffsetDateTime.parse(START_AT), OffsetDateTime.parse(END_AT), now,
                    1L, "독서", 1L, "서울",
                    ActivityType.OFFLINE, true, 20, 30, Gender.ANY
            );

            given(activityService.createActivity(eq(1L), any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.ofEntries(
                                    Map.entry("openType", "PUBLIC"),
                                    Map.entry("name", "봄맞이 독서 모임"),
                                    Map.entry("description", "함께 책을 읽어요"),
                                    Map.entry("capacity", 20),
                                    Map.entry("isAttendanceCheck", false),
                                    Map.entry("isSearchable", true),
                                    Map.entry("startAt", START_AT),
                                    Map.entry("endAt", END_AT),
                                    Map.entry("categoryId", 1),
                                    Map.entry("regionId", 1),
                                    Map.entry("activityType", "OFFLINE"),
                                    Map.entry("isVerificationRequired", true),
                                    Map.entry("minAge", 20),
                                    Map.entry("maxAge", 30),
                                    Map.entry("gender", "ANY")
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
                    .andExpect(jsonPath("$.result.isSearchable").value(true))
                    .andExpect(jsonPath("$.result.categoryId").value(1))
                    .andExpect(jsonPath("$.result.categoryName").value("독서"))
                    .andExpect(jsonPath("$.result.regionId").value(1))
                    .andExpect(jsonPath("$.result.regionName").value("서울"))
                    .andExpect(jsonPath("$.result.activityType").value("OFFLINE"))
                    .andExpect(jsonPath("$.result.isVerificationRequired").value(true))
                    .andExpect(jsonPath("$.result.minAge").value(20))
                    .andExpect(jsonPath("$.result.maxAge").value(30))
                    .andExpect(jsonPath("$.result.gender").value("ANY"));
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
                    OffsetDateTime.parse(START_AT), OffsetDateTime.parse(END_AT), now,
                    null, null, null, null,
                    ActivityType.BOTH, false, null, null, Gender.ANY
            );

            given(activityService.createActivity(eq(1L), any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/activities")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.ofEntries(
                                    Map.entry("clubId", 10),
                                    Map.entry("openType", "PRIVATE"),
                                    Map.entry("name", "정기 모임"),
                                    Map.entry("description", "이번 주 정기 모임"),
                                    Map.entry("isAttendanceCheck", true),
                                    Map.entry("isSearchable", false),
                                    Map.entry("startAt", START_AT),
                                    Map.entry("endAt", END_AT),
                                    Map.entry("activityType", "BOTH"),
                                    Map.entry("gender", "ANY")
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.activityId").value(101))
                    .andExpect(jsonPath("$.result.clubId").value(10))
                    .andExpect(jsonPath("$.result.clubName").value("테스트 동아리"))
                    .andExpect(jsonPath("$.result.openType").value("PRIVATE"))
                    .andExpect(jsonPath("$.result.isAttendanceCheck").value(true))
                    .andExpect(jsonPath("$.result.isSearchable").value(false))
                    .andExpect(jsonPath("$.result.activityType").value("BOTH"))
                    .andExpect(jsonPath("$.result.gender").value("ANY"));
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
                                    "activityType", "BOTH",
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
                                    "activityType", "BOTH",
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
                                    "activityType", "BOTH",
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
                                    "activityType", "BOTH",
                                    "startAt", START_AT,
                                    "endAt", END_AT
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(6000));
        }
    }

    @Nested
    @DisplayName("DELETE /activities/{activityId} - 모임 삭제")
    class DeleteActivity {

        @Test
        @DisplayName("성공: 모임 삭제")
        void successDeleteActivity() throws Exception {
            // when & then
            mockMvc.perform(delete("/activities/{activityId}", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(delete("/activities/{activityId}", 100L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 모임 없음 (서비스 예외)")
        void failWithActivityNotFound() throws Exception {
            // given
            willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND))
                    .given(activityService).deleteActivity(eq(1L), eq(999L));

            // when & then
            mockMvc.perform(delete("/activities/{activityId}", 999L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5000));
        }

        @Test
        @DisplayName("실패: 삭제 권한 없음 (서비스 예외)")
        void failWithPermissionDenied() throws Exception {
            // given
            willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED))
                    .given(activityService).deleteActivity(eq(1L), eq(100L));

            // when & then
            mockMvc.perform(delete("/activities/{activityId}", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(5004));
        }
    }

    @Nested
    @DisplayName("GET /activities/{activityId} - 모임 상세 조회")
    class GetActivityDetail {

        @Test
        @DisplayName("성공: 모임 상세 조회")
        void successGetActivityDetail() throws Exception {
            // given
            List<GetActivityDetailResult.ParticipantInfo> participants = List.of(
                    new GetActivityDetailResult.ParticipantInfo(2L, "img1.jpg", "참가자1"),
                    new GetActivityDetailResult.ParticipantInfo(3L, null, "참가자2")
            );

            GetActivityDetailResult result = new GetActivityDetailResult(
                    100L, "봄맞이 독서 모임", "함께 책을 읽어요",
                    10L, "테스트 동아리", OpenType.PUBLIC,
                    20, OffsetDateTime.parse(START_AT), OffsetDateTime.parse(END_AT),
                    participants
            );

            given(activityService.getActivityDetail(eq(100L))).willReturn(result);

            // when & then
            mockMvc.perform(get("/activities/{activityId}", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.activityId").value(100))
                    .andExpect(jsonPath("$.result.name").value("봄맞이 독서 모임"))
                    .andExpect(jsonPath("$.result.description").value("함께 책을 읽어요"))
                    .andExpect(jsonPath("$.result.clubId").value(10))
                    .andExpect(jsonPath("$.result.clubName").value("테스트 동아리"))
                    .andExpect(jsonPath("$.result.openType").value("PUBLIC"))
                    .andExpect(jsonPath("$.result.capacity").value(20))
                    .andExpect(jsonPath("$.result.participants").isArray())
                    .andExpect(jsonPath("$.result.participants.length()").value(2))
                    .andExpect(jsonPath("$.result.participants[0].id").value(2))
                    .andExpect(jsonPath("$.result.participants[0].profileImg").value("img1.jpg"))
                    .andExpect(jsonPath("$.result.participants[0].name").value("참가자1"))
                    .andExpect(jsonPath("$.result.participants[1].id").value(3))
                    .andExpect(jsonPath("$.result.participants[1].name").value("참가자2"));
        }

        @Test
        @DisplayName("성공: 개인 모임 상세 조회 (clubId/clubName null)")
        void successGetPersonalActivityDetail() throws Exception {
            // given
            GetActivityDetailResult result = new GetActivityDetailResult(
                    100L, "개인 모임", "개인 모임입니다",
                    null, null, OpenType.PRIVATE,
                    null, OffsetDateTime.parse(START_AT), OffsetDateTime.parse(END_AT),
                    Collections.emptyList()
            );

            given(activityService.getActivityDetail(eq(100L))).willReturn(result);

            // when & then
            mockMvc.perform(get("/activities/{activityId}", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.clubId").doesNotExist())
                    .andExpect(jsonPath("$.result.clubName").doesNotExist())
                    .andExpect(jsonPath("$.result.capacity").doesNotExist())
                    .andExpect(jsonPath("$.result.participants").isArray())
                    .andExpect(jsonPath("$.result.participants.length()").value(0));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(get("/activities/{activityId}", 100L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 모임 없음 (서비스 예외)")
        void failWithActivityNotFound() throws Exception {
            // given
            given(activityService.getActivityDetail(eq(999L)))
                    .willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/activities/{activityId}", 999L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5000));
        }
    }

    @Nested
    @DisplayName("GET /activities/{activityId}/participants - 지원자 리스트 조회")
    class GetParticipantList {

        @Test
        @DisplayName("성공: 지원자 리스트 조회")
        void successGetParticipantList() throws Exception {
            // given
            List<ParticipantResult> content = List.of(
                    new ParticipantResult(1L, 2L, "img.jpg", "홍길동", ParticipantStatus.PENDING,
                            OffsetDateTime.parse("2026-03-01T10:00:00+09:00")),
                    new ParticipantResult(2L, 3L, null, "김철수", ParticipantStatus.APPROVED,
                            OffsetDateTime.parse("2026-03-02T10:00:00+09:00"))
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            given(activityService.getParticipantList(eq(1L), eq(100L), any(Pageable.class))).willReturn(slice);

            // when & then
            mockMvc.perform(get("/activities/{activityId}/participants", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content.length()").value(2))
                    .andExpect(jsonPath("$.result.content[0].participantId").value(1))
                    .andExpect(jsonPath("$.result.content[0].userId").value(2))
                    .andExpect(jsonPath("$.result.content[0].profileImg").value("img.jpg"))
                    .andExpect(jsonPath("$.result.content[0].name").value("홍길동"))
                    .andExpect(jsonPath("$.result.content[0].participantStatus").value("PENDING"))
                    .andExpect(jsonPath("$.result.content[1].participantId").value(2))
                    .andExpect(jsonPath("$.result.content[1].participantStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.result.page").value(0))
                    .andExpect(jsonPath("$.result.size").value(10))
                    .andExpect(jsonPath("$.result.hasNext").value(false));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(get("/activities/{activityId}/participants", 100L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 모임 없음 (서비스 예외)")
        void failWithActivityNotFound() throws Exception {
            // given
            given(activityService.getParticipantList(eq(1L), eq(999L), any(Pageable.class)))
                    .willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/activities/{activityId}/participants", 999L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5000));
        }

        @Test
        @DisplayName("실패: 조회 권한 없음 (서비스 예외)")
        void failWithPermissionDenied() throws Exception {
            // given
            given(activityService.getParticipantList(eq(1L), eq(100L), any(Pageable.class)))
                    .willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_VIEW_PERMISSION_DENIED));

            // when & then
            mockMvc.perform(get("/activities/{activityId}/participants", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(5007));
        }
    }

    @Nested
    @DisplayName("PATCH /activities/{activityId}/participants/{participantId}/status - 지원자 상태 변경")
    class UpdateParticipantStatus {

        @Test
        @DisplayName("성공: 지원자 확정")
        void successApprove() throws Exception {
            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participants/{participantId}/status", 100L, 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "APPROVED"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("성공: 지원자 거절")
        void successReject() throws Exception {
            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participants/{participantId}/status", 100L, 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "REJECTED"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participants/{participantId}/status", 100L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "APPROVED"
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: participantStatus 누락")
        void failWithMissingStatus() throws Exception {
            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participants/{participantId}/status", 100L, 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 모임 없음 (서비스 예외)")
        void failWithActivityNotFound() throws Exception {
            // given
            willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND))
                    .given(activityService).updateParticipantStatus(eq(1L), eq(999L), eq(1L), any());

            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participants/{participantId}/status", 999L, 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "APPROVED"
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5000));
        }

        @Test
        @DisplayName("실패: 관리 권한 없음 (서비스 예외)")
        void failWithPermissionDenied() throws Exception {
            // given
            willThrow(new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_MANAGE_PERMISSION_DENIED))
                    .given(activityService).updateParticipantStatus(eq(1L), eq(100L), eq(1L), any());

            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participants/{participantId}/status", 100L, 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "APPROVED"
                            ))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(5009));
        }

        @Test
        @DisplayName("실패: 지원자 없음 (서비스 예외)")
        void failWithParticipantNotFound() throws Exception {
            // given
            willThrow(new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_FOUND))
                    .given(activityService).updateParticipantStatus(eq(1L), eq(100L), eq(999L), any());

            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participants/{participantId}/status", 100L, 999L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "APPROVED"
                            ))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5008));
        }
    }

    @Nested
    @DisplayName("PATCH /activities/{activityId}/participation - 모임 참여 응답")
    class RespondToParticipation {

        @Test
        @DisplayName("성공: 참여 응답")
        void successRespondToParticipation() throws Exception {
            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participation", 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "CONFIRMED"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participation", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "participantStatus", "CONFIRMED"
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: participantStatus 누락")
        void failWithMissingStatus() throws Exception {
            // when & then
            mockMvc.perform(patch("/activities/{activityId}/participation", 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /activities/{activityId}/interest - 관심 모임 추가")
    class AddActivityInterest {

        @Test
        @DisplayName("성공: 관심 모임 추가")
        void successAddInterest() throws Exception {
            // when & then
            mockMvc.perform(post("/activities/{activityId}/interest", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(post("/activities/{activityId}/interest", 100L))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /activities/{activityId}/interest - 관심 모임 제거")
    class RemoveActivityInterest {

        @Test
        @DisplayName("성공: 관심 모임 제거")
        void successRemoveInterest() throws Exception {
            // when & then
            mockMvc.perform(delete("/activities/{activityId}/interest", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(delete("/activities/{activityId}/interest", 100L))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /activities/interests - 관심 모임 목록 조회")
    class GetInterestedActivities {

        @Test
        @DisplayName("성공: 관심 모임 목록 조회")
        void successGetInterestedActivities() throws Exception {
            // given
            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(1L, "thumb.jpg", "PERSONAL", null, "관심 모임",
                            "카테고리", "서울", 5, 20, 10, 100, 3, true)
            );
            given(activityService.getInterestedActivities(eq(1L), any(Pageable.class)))
                    .willReturn(new SliceImpl<>(content, PageRequest.of(0, 10), false));

            // when & then
            mockMvc.perform(get("/activities/interests")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content[0].name").value("관심 모임"))
                    .andExpect(jsonPath("$.result.content[0].isLiked").value(true));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(get("/activities/interests"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /activities/{activityId}/invite-code - 초대 코드 조회")
    class GetInviteCode {

        @Test
        @DisplayName("성공: 초대 코드 조회")
        void success() throws Exception {
            // given
            given(activityService.getInviteCode(1L, 100L)).willReturn("test-invite-code-uuid");

            // when & then
            mockMvc.perform(get("/activities/{activityId}/invite-code", 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.inviteCode").value("test-invite-code-uuid"));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(get("/activities/{activityId}/invite-code", 100L))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /activities/join - 초대 코드로 모임 참여")
    class JoinByInviteCode {

        @Test
        @DisplayName("성공: 초대 코드로 모임 참여")
        void success() throws Exception {
            // when & then
            mockMvc.perform(post("/activities/join")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("inviteCode", "test-invite-code"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청")
        void failWithoutAuthentication() throws Exception {
            // when & then
            mockMvc.perform(post("/activities/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("inviteCode", "test-invite-code"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: inviteCode 누락")
        void failMissingInviteCode() throws Exception {
            // when & then
            mockMvc.perform(post("/activities/join")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: inviteCode 빈 문자열")
        void failEmptyInviteCode() throws Exception {
            // when & then
            mockMvc.perform(post("/activities/join")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("inviteCode", ""))))
                    .andExpect(status().isBadRequest());
        }
    }
}
