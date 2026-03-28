package com.dewple.app_api_auth.api.club.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.service.ClubDetailResult;
import com.dewple.club.service.ClubService;
import com.dewple.club.service.ClubSummaryResult;
import com.dewple.club.service.CreateClubResult;
import com.dewple.club.service.GetClubListParam;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubController.class)
@Import(SecurityConfig.class)
class ClubControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ClubService clubService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("GET /clubs - 동아리 목록 조회")
    class GetClubList {

        @Test
        @DisplayName("성공: 필터 없이 목록 조회")
        void success() throws Exception {
            List<ClubSummaryResult> content = List.of(
                    new ClubSummaryResult(1L, "코딩 동아리", null, ActivityType.BOTH, false, 10, 50L),
                    new ClubSummaryResult(2L, "등산 동아리", "cover.jpg", ActivityType.OFFLINE, false, 5, 30L)
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);
            given(clubService.getClubList(any(GetClubListParam.class))).willReturn(slice);

            mockMvc.perform(get("/clubs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content.length()").value(2))
                    .andExpect(jsonPath("$.result.content[0].id").value(1))
                    .andExpect(jsonPath("$.result.content[0].name").value("코딩 동아리"))
                    .andExpect(jsonPath("$.result.content[0].memberCount").value(50))
                    .andExpect(jsonPath("$.result.hasNext").value(false));
        }

        @Test
        @DisplayName("성공: 필터 조합 조회")
        void successWithFilters() throws Exception {
            var slice = new SliceImpl<>(
                    List.of(new ClubSummaryResult(1L, "동아리", null, ActivityType.OFFLINE, true, 3, 10L)),
                    PageRequest.of(0, 10), false
            );
            given(clubService.getClubList(any(GetClubListParam.class))).willReturn(slice);

            mockMvc.perform(get("/clubs")
                            .param("isVerificationRequired", "true")
                            .param("categoryId", "1")
                            .param("regionId", "2")
                            .param("activityType", "OFFLINE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content.length()").value(1))
                    .andExpect(jsonPath("$.result.content[0].isVerificationRequired").value(true));
        }

        @Test
        @DisplayName("성공: 인증 없이 조회 가능 (공개 API)")
        void successWithoutAuth() throws Exception {
            var slice = new SliceImpl<>(List.<ClubSummaryResult>of(), PageRequest.of(0, 10), false);
            given(clubService.getClubList(any(GetClubListParam.class))).willReturn(slice);

            mockMvc.perform(get("/clubs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId} - 동아리 단건 조회")
    class GetClubDetail {

        @Test
        @DisplayName("성공: 동아리 상세 조회")
        void success() throws Exception {
            ClubDetailResult result = new ClubDetailResult(
                    1L, "코딩 동아리", "코딩하는 동아리", "cover.jpg",
                    "{\"components\":[]}", ActivityType.BOTH, false,
                    Gender.ANY, null, null, LocalDate.of(2024, 1, 1),
                    10, BigDecimal.valueOf(4.5), 100L
            );
            given(clubService.getClubDetail(eq(1L))).willReturn(result);

            mockMvc.perform(get("/clubs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.id").value(1))
                    .andExpect(jsonPath("$.result.name").value("코딩 동아리"))
                    .andExpect(jsonPath("$.result.landingPage").value("{\"components\":[]}"))
                    .andExpect(jsonPath("$.result.activityType").value("BOTH"))
                    .andExpect(jsonPath("$.result.likeCount").value(10))
                    .andExpect(jsonPath("$.result.creatorId").value(100));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 동아리")
        void failNotFound() throws Exception {
            given(clubService.getClubDetail(eq(999L)))
                    .willThrow(new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

            mockMvc.perform(get("/clubs/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("성공: 인증 없이 조회 가능 (공개 API)")
        void successWithoutAuth() throws Exception {
            ClubDetailResult result = new ClubDetailResult(
                    1L, "동아리", null, null, null, ActivityType.BOTH,
                    false, Gender.ANY, null, null, null, 0,
                    BigDecimal.valueOf(5.0), 100L
            );
            given(clubService.getClubDetail(eq(1L))).willReturn(result);

            mockMvc.perform(get("/clubs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }
    }

    @Nested
    @DisplayName("POST /clubs - 동아리 생성")
    class CreateClub {

        @Test
        @DisplayName("성공: 동아리 생성")
        void success() throws Exception {
            CreateClubResult result = new CreateClubResult(
                    100L, "코딩 동아리", false, ActivityType.BOTH,
                    LocalDate.of(2024, 1, 1), List.of(1L, 2L), List.of(1L), 1L
            );
            given(clubService.createClub(eq(1L), any())).willReturn(result);

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "코딩 동아리",
                    "isVerificationRequired", false,
                    "activityType", "BOTH",
                    "foundedDate", "2024-01-01",
                    "categoryIds", List.of(1, 2),
                    "regionIds", List.of(1)
            ));

            mockMvc.perform(post("/clubs")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.clubId").value(100))
                    .andExpect(jsonPath("$.result.name").value("코딩 동아리"))
                    .andExpect(jsonPath("$.result.activityType").value("BOTH"))
                    .andExpect(jsonPath("$.result.creatorId").value(1));
        }

        @Test
        @DisplayName("실패: 이름 누락")
        void failNameBlank() throws Exception {
            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "",
                    "isVerificationRequired", false,
                    "activityType", "BOTH",
                    "categoryIds", List.of(1),
                    "regionIds", List.of(1)
            ));

            mockMvc.perform(post("/clubs")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 회장 동시 운영 초과")
        void failPresidentLimit() throws Exception {
            given(clubService.createClub(eq(1L), any()))
                    .willThrow(new BusinessException(ClubErrorCode.CLUB_PRESIDENT_LIMIT_EXCEEDED));

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "동아리",
                    "isVerificationRequired", false,
                    "activityType", "BOTH",
                    "categoryIds", List.of(1),
                    "regionIds", List.of(1)
            ));

            mockMvc.perform(post("/clubs")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 없음")
        void failNoAuth() throws Exception {
            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "동아리",
                    "isVerificationRequired", false,
                    "activityType", "BOTH",
                    "categoryIds", List.of(1),
                    "regionIds", List.of(1)
            ));

            mockMvc.perform(post("/clubs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }
    }
}
