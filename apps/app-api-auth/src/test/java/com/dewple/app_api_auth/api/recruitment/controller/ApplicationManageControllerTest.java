package com.dewple.app_api_auth.api.recruitment.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.service.ApplicationDetailResult;
import com.dewple.recruitment.service.ApplicationListResult;
import com.dewple.recruitment.service.ApplicationManageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationManageController.class)
@Import(SecurityConfig.class)
class ApplicationManageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationManageService applicationManageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    @Nested
    @DisplayName("GET /clubs/{clubId}/recruitment-posts/{postingId}/applications — 지원자 목록 조회")
    class GetApplicationList {

        @Test
        @DisplayName("성공: 지원자 목록 조회")
        void success() throws Exception {
            // given
            ApplicationListResult result = new ApplicationListResult(
                    500L, "홍길동", "01011112222", NOW, ApplicationStatus.SUBMITTED);

            given(applicationManageService.getApplicationList(eq(1L), eq(1L), eq(100L), eq(null), eq(null), any()))
                    .willReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 20), 1));

            // when & then
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content[0].applicationId").value(500))
                    .andExpect(jsonPath("$.result.content[0].applicantName").value("홍길동"))
                    .andExpect(jsonPath("$.result.totalElements").value(1))
                    .andExpect(jsonPath("$.result.page").value(0))
                    .andExpect(jsonPath("$.result.size").value(20));
        }

        @Test
        @DisplayName("성공: 상태 필터로 조회")
        void successWithStatusFilter() throws Exception {
            // given
            ApplicationListResult result = new ApplicationListResult(
                    500L, "홍길동", "01011112222", NOW, ApplicationStatus.ACCEPTED);

            given(applicationManageService.getApplicationList(eq(1L), eq(1L), eq(100L), eq(ApplicationStatus.ACCEPTED), eq(null), any()))
                    .willReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 20), 1));

            // when & then
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("status", "ACCEPTED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content[0].applicationStatus").value("ACCEPTED"));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 공고를 찾을 수 없음 → 404")
        void failPostingNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND))
                    .given(applicationManageService).getApplicationList(eq(1L), eq(1L), eq(100L), eq(null), eq(null), any());

            // when & then
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/{postingId}/applications", 1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5007));
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId} — 지원자 상세 조회")
    class GetApplicationDetail {

        @Test
        @DisplayName("성공: 지원자 상세 조회")
        void success() throws Exception {
            // given
            ApplicationDetailResult result = new ApplicationDetailResult(
                    500L, "홍길동", "01011112222",
                    "[{\"key\":\"q1\",\"value\":\"답변\"}]",
                    NOW, ApplicationStatus.SUBMITTED);

            given(applicationManageService.getApplicationDetail(1L, 1L, 100L, 500L))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.applicationId").value(500))
                    .andExpect(jsonPath("$.result.applicantName").value("홍길동"))
                    .andExpect(jsonPath("$.result.answers").isArray())
                    .andExpect(jsonPath("$.result.applicationStatus").value("SUBMITTED"));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 지원서를 찾을 수 없음 → 404")
        void failNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND))
                    .given(applicationManageService).getApplicationDetail(1L, 1L, 100L, 500L);

            // when & then
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5100));
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}/status — 단건 상태 변경")
    class ChangeApplicationStatus {

        @Test
        @DisplayName("성공: 합격 처리")
        void successAccepted() throws Exception {
            // given
            willDoNothing().given(applicationManageService)
                    .changeApplicationStatus(1L, 1L, 100L, 500L, ApplicationStatus.ACCEPTED);

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}/status",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("applicationStatus", "ACCEPTED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));

            verify(applicationManageService).changeApplicationStatus(1L, 1L, 100L, 500L, ApplicationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("성공: 불합격 처리")
        void successRejected() throws Exception {
            // given
            willDoNothing().given(applicationManageService)
                    .changeApplicationStatus(1L, 1L, 100L, 500L, ApplicationStatus.REJECTED);

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}/status",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("applicationStatus", "REJECTED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));

            verify(applicationManageService).changeApplicationStatus(1L, 1L, 100L, 500L, ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}/status",
                            1L, 100L, 500L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("applicationStatus", "ACCEPTED"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 지원서를 찾을 수 없음 → 404")
        void failNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND))
                    .given(applicationManageService).changeApplicationStatus(1L, 1L, 100L, 500L, ApplicationStatus.ACCEPTED);

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}/status",
                            1L, 100L, 500L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("applicationStatus", "ACCEPTED"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5100));
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/recruitment-posts/{postingId}/applications/status — 일괄 상태 변경")
    class BatchChangeApplicationStatus {

        @Test
        @DisplayName("성공: 일괄 합격 처리")
        void success() throws Exception {
            // given
            willDoNothing().given(applicationManageService)
                    .batchChangeApplicationStatus(eq(1L), eq(1L), eq(100L), eq(List.of(501L, 502L)), eq(ApplicationStatus.ACCEPTED));

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/status",
                            1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("applicationIds", List.of(501, 502),
                                            "applicationStatus", "ACCEPTED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));

            verify(applicationManageService).batchChangeApplicationStatus(
                    eq(1L), eq(1L), eq(100L), eq(List.of(501L, 502L)), eq(ApplicationStatus.ACCEPTED));
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/status",
                            1L, 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("applicationIds", List.of(501, 502),
                                            "applicationStatus", "ACCEPTED"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 일부 지원서를 찾을 수 없음 → 400")
        void failSomeNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.APPLICATION_SOME_NOT_FOUND))
                    .given(applicationManageService).batchChangeApplicationStatus(
                            eq(1L), eq(1L), eq(100L), eq(List.of(501L, 999L)), eq(ApplicationStatus.ACCEPTED));

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}/applications/status",
                            1L, 100L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("applicationIds", List.of(501, 999),
                                            "applicationStatus", "ACCEPTED"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5110));
        }
    }
}
