package com.dewple.app_api_auth.api.club.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.service.ClubService;
import com.dewple.club.service.CreateClubResult;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.exception.BusinessException;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
