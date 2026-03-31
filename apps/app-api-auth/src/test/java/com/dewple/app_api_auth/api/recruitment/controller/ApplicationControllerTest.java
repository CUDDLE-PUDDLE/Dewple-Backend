package com.dewple.app_api_auth.api.recruitment.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.entity.RecruitmentSchema;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.service.ApplicationService;
import com.dewple.recruitment.service.MyApplicationListResult;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("POST /clubs/{clubId}/recruitment-posts/{postingId}/applications — 지원서 제출")
    class SubmitApplication {

        @Test
        @DisplayName("성공: 유효한 요청으로 지원서 제출")
        void success() throws Exception {
            // given
            Application application = createMockApplication(500L, ApplicationStatus.SUBMITTED);
            given(applicationService.submitApplication(eq(1L), eq(100L), eq(1L), any()))
                    .willReturn(application);

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.applicationId").value(500))
                    .andExpect(jsonPath("$.result.applicationStatus").value("SUBMITTED"));

            verify(applicationService).submitApplication(eq(1L), eq(100L), eq(1L), any());
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 서비스에서 APPLICATION_ALREADY_SUBMITTED → 409, code=5101")
        void failAlreadySubmitted() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_ALREADY_SUBMITTED))
                    .given(applicationService).submitApplication(eq(1L), eq(100L), eq(1L), any());

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(5202));
        }

        @Test
        @DisplayName("실패: 서비스에서 POSTING_NOT_ACCEPTING → 400, code=5103")
        void failPostingNotAccepting() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_NOT_ACCEPTING))
                    .given(applicationService).submitApplication(eq(1L), eq(100L), eq(1L), any());

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5204));
        }
    }

    @Nested
    @DisplayName("PUT /clubs/{clubId}/recruitment-posts/{postingId}/applications — 지원서 임시저장")
    class TemporarySaveApplication {

        @Test
        @DisplayName("성공: 임시저장")
        void success() throws Exception {
            // given
            Application application = createMockApplication(500L, ApplicationStatus.TEMPORARY);
            given(applicationService.temporarySaveApplication(eq(1L), eq(100L), eq(1L), any()))
                    .willReturn(application);

            // when & then
            mockMvc.perform(put("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.applicationId").value(500))
                    .andExpect(jsonPath("$.result.applicationStatus").value("TEMPORARY"));

            verify(applicationService).temporarySaveApplication(eq(1L), eq(100L), eq(1L), any());
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(put("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId} — 지원 철회")
    class WithdrawApplication {

        @Test
        @DisplayName("성공: 지원 철회")
        void success() throws Exception {
            // when & then
            mockMvc.perform(delete("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));

            verify(applicationService).withdrawApplication(100L, 500L, 1L);
        }

        @Test
        @DisplayName("실패: 서비스에서 APPLICATION_NOT_OWNER → 403, code=5105")
        void failNotOwner() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_OWNER))
                    .given(applicationService).withdrawApplication(100L, 500L, 1L);

            // when & then
            mockMvc.perform(delete("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(5206));
        }

        @Test
        @DisplayName("실패: 서비스에서 APPLICATION_NOT_WITHDRAWABLE → 400, code=5102")
        void failNotWithdrawable() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_WITHDRAWABLE))
                    .given(applicationService).withdrawApplication(100L, 500L, 1L);

            // when & then
            mockMvc.perform(delete("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5203));
        }
    }

    @Nested
    @DisplayName("GET /users/me/applications — 내 지원 내역 조회")
    class GetMyApplications {

        @Test
        @DisplayName("성공: 내 지원 내역 조회")
        void success() throws Exception {
            // given
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            MyApplicationListResult result = new MyApplicationListResult(
                    500L, 100L, 1L, "테스트 동아리", "테스트 공고",
                    ApplicationStatus.SUBMITTED, now, now
            );
            given(applicationService.getMyApplications(1L)).willReturn(List.of(result));

            // when & then
            mockMvc.perform(get("/users/me/applications")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result[0].applicationId").value(500))
                    .andExpect(jsonPath("$.result[0].clubName").value("테스트 동아리"))
                    .andExpect(jsonPath("$.result[0].applicationStatus").value("SUBMITTED"));

            verify(applicationService).getMyApplications(1L);
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(get("/users/me/applications"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId} — 지원서 수정")
    class EditApplicationTest {

        @Test
        @DisplayName("성공: 지원서 수정")
        void success() throws Exception {
            // given
            Application application = createMockApplication(500L, ApplicationStatus.SUBMITTED);
            given(applicationService.editApplication(eq(1L), eq(100L), eq(500L), eq(1L), any()))
                    .willReturn(application);

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.applicationId").value(500))
                    .andExpect(jsonPath("$.result.applicationStatus").value("SUBMITTED"));

            verify(applicationService).editApplication(eq(1L), eq(100L), eq(500L), eq(1L), any());
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 서비스에서 APPLICATION_NOT_EDITABLE → 400, code=5106")
        void failNotEditable() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_EDITABLE))
                    .given(applicationService).editApplication(eq(1L), eq(100L), eq(500L), eq(1L), any());

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5207));
        }

        @Test
        @DisplayName("실패: 서비스에서 APPLICATION_EDIT_WINDOW_CLOSED → 400, code=5107")
        void failEditWindowClosed() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_EDIT_WINDOW_CLOSED))
                    .given(applicationService).editApplication(eq(1L), eq(100L), eq(500L), eq(1L), any());

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5208));
        }
    }

    // ── Helper Methods ──

    private Application createMockApplication(Long id, ApplicationStatus status) {
        RecruitmentSchema schema = RecruitmentSchema.builder()
                .version(1L)
                .applicationForm("{}")
                .build();
        ReflectionTestUtils.setField(schema, "id", 200L);

        Application application = Application.builder()
                .recruitmentSchema(schema)
                .answers("[{\"key\":\"q1\",\"value\":\"답변\"}]")
                .applicationStatus(status)
                .build();
        ReflectionTestUtils.setField(application, "id", id);
        return application;
    }

    private Map<String, Object> createValidRequest() {
        return Map.of(
                "answers", List.of(
                        Map.of("key", "q1", "value", "자기소개입니다.")
                )
        );
    }

}
