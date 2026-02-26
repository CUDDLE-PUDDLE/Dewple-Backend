package com.dewple.app_api_auth.api.recruitment.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.RecruitmentPosting;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.service.RecruitmentService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecruitmentController.class)
@Import(SecurityConfig.class)
class RecruitmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecruitmentService recruitmentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("POST /clubs/{clubId}/recruitment-posts — 공고 생성")
    class CreateRecruitment {

        @Test
        @DisplayName("성공: 유효한 요청으로 공고 생성")
        void success() throws Exception {
            // given
            RecruitmentPosting posting = createMockPosting(100L, 1L);
            given(recruitmentService.createRecruitment(eq(1L), eq(1L), any())).willReturn(posting);

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts", 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("요청에 성공하였습니다."))
                    .andExpect(jsonPath("$.result.postingId").value(100))
                    .andExpect(jsonPath("$.result.version").value(1));

            verify(recruitmentService).createRecruitment(eq(1L), eq(1L), any());
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 필수 필드 누락 (title blank) → 400")
        void failWithBlankTitle() throws Exception {
            // given
            Map<String, Object> request = createValidRequest();
            request.put("title", "");

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts", 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 서비스에서 GENERATION_NOT_FOUND → 404, code=5001")
        void failWithGenerationNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.GENERATION_NOT_FOUND))
                    .given(recruitmentService).createRecruitment(eq(1L), eq(1L), any());

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts", 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5001));
        }
    }

    @Nested
    @DisplayName("PUT /clubs/{clubId}/recruitment-posts — 임시 저장")
    class TemporaryStorageRecruitment {

        @Test
        @DisplayName("성공: postingId 없이 새 초안 생성")
        void successNewDraft() throws Exception {
            // given
            RecruitmentPosting posting = createMockPosting(100L, 0L);
            given(recruitmentService.temporaryStorageRecruitment(eq(1L), eq(1L), isNull(), any()))
                    .willReturn(posting);

            // when & then
            mockMvc.perform(put("/clubs/{clubId}/recruitment-posts", 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.postingId").value(100))
                    .andExpect(jsonPath("$.result.version").value(0));

            verify(recruitmentService).temporaryStorageRecruitment(eq(1L), eq(1L), isNull(), any());
        }

        @Test
        @DisplayName("성공: postingId 있는 기존 초안 업데이트")
        void successUpdateDraft() throws Exception {
            // given
            RecruitmentPosting posting = createMockPosting(10L, 0L);
            given(recruitmentService.temporaryStorageRecruitment(eq(1L), eq(1L), eq(10L), any()))
                    .willReturn(posting);

            // when & then
            mockMvc.perform(put("/clubs/{clubId}/recruitment-posts", 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("postingId", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.postingId").value(10))
                    .andExpect(jsonPath("$.result.version").value(0));

            verify(recruitmentService).temporaryStorageRecruitment(eq(1L), eq(1L), eq(10L), any());
        }

        @Test
        @DisplayName("실패: 서비스에서 POSTING_NOT_DRAFT → 400, code=5008")
        void failWithPostingNotDraft() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_NOT_DRAFT))
                    .given(recruitmentService)
                    .temporaryStorageRecruitment(eq(1L), eq(1L), eq(10L), any());

            // when & then
            mockMvc.perform(put("/clubs/{clubId}/recruitment-posts", 1L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("postingId", "10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5008));
        }
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/recruitment-posts/{postingId}/publish — 공고 발행")
    class PublishRecruitment {

        @Test
        @DisplayName("성공: 공고 발행")
        void success() throws Exception {
            // given
            RecruitmentPosting posting = createMockPosting(10L, 1L);
            given(recruitmentService.publishRecruitment(1L, 1L, 10L)).willReturn(posting);

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/publish", 1L, 10L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.postingId").value(10))
                    .andExpect(jsonPath("$.result.version").value(1));

            verify(recruitmentService).publishRecruitment(1L, 1L, 10L);
        }

        @Test
        @DisplayName("실패: 서비스에서 POSTING_NOT_FOUND → 404, code=5007")
        void failWithPostingNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND))
                    .given(recruitmentService).publishRecruitment(1L, 1L, 999L);

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/publish", 1L, 999L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5007));
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/recruitment-posts/{postingId} — 공고 수정")
    class UpdateRecruitment {

        @Test
        @DisplayName("성공: 유효한 요청으로 공고 수정")
        void success() throws Exception {
            // given
            RecruitmentPosting posting = createMockPosting(10L, 2L);
            given(recruitmentService.updateRecruitment(eq(1L), eq(1L), eq(10L), any())).willReturn(posting);

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}", 1L, 10L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.postingId").value(10))
                    .andExpect(jsonPath("$.result.version").value(2));

            verify(recruitmentService).updateRecruitment(eq(1L), eq(1L), eq(10L), any());
        }

        @Test
        @DisplayName("실패: 서비스에서 POSTING_NOT_OPEN → 400, code=5010")
        void failWithPostingNotOpen() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_NOT_OPEN))
                    .given(recruitmentService).updateRecruitment(eq(1L), eq(1L), eq(10L), any());

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}", 1L, 10L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidUpdateRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5010));
        }

        @Test
        @DisplayName("실패: 서비스에서 FORM_COMPONENT_REMOVAL_NOT_ALLOWED → 400, code=5009")
        void failWithFormComponentRemovalNotAllowed() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.FORM_COMPONENT_REMOVAL_NOT_ALLOWED))
                    .given(recruitmentService).updateRecruitment(eq(1L), eq(1L), eq(10L), any());

            // when & then
            mockMvc.perform(patch("/clubs/{clubId}/recruitment-posts/{postingId}", 1L, 10L)
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createValidUpdateRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5009));
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/recruitment-posts/previous-form — 이전 공고 지원 양식 불러오기")
    class GetPreviousApplicationForm {

        @Test
        @DisplayName("성공: 이전 공고의 지원 양식 반환")
        void success() throws Exception {
            // given
            String applicationForm = "{\"textarea\":[{\"key\":\"q1\",\"question\":\"자기소개\"}],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}";
            given(recruitmentService.getPreviousApplicationForm(1L, 1L)).willReturn(applicationForm);

            // when & then
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/previous-form", 1L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result").isNotEmpty());

            verify(recruitmentService).getPreviousApplicationForm(1L, 1L);
        }

        @Test
        @DisplayName("성공: 이전 공고가 없으면 result가 null")
        void successWithNoPreviousPosting() throws Exception {
            // given
            given(recruitmentService.getPreviousApplicationForm(1L, 1L)).willReturn(null);

            // when & then
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/previous-form", 1L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result").doesNotExist());

            verify(recruitmentService).getPreviousApplicationForm(1L, 1L);
        }

        @Test
        @DisplayName("실패: 인증 없이 요청 → 401")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(get("/clubs/{clubId}/recruitment-posts/previous-form", 1L))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /clubs/{clubId}/recruitment-posts/{postingId}/close — 공고 마감")
    class CloseRecruitment {

        @Test
        @DisplayName("성공: 공고 마감")
        void success() throws Exception {
            // given
            RecruitmentPosting posting = createMockPosting(10L, 1L);
            given(recruitmentService.closeRecruitment(1L, 1L, 10L)).willReturn(posting);

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/close", 1L, 10L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.postingId").value(10))
                    .andExpect(jsonPath("$.result.version").value(1));

            verify(recruitmentService).closeRecruitment(1L, 1L, 10L);
        }

        @Test
        @DisplayName("실패: 서비스에서 POSTING_ALREADY_CLOSED → 400, code=5011")
        void failWithPostingAlreadyClosed() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_ALREADY_CLOSED))
                    .given(recruitmentService).closeRecruitment(1L, 1L, 10L);

            // when & then
            mockMvc.perform(post("/clubs/{clubId}/recruitment-posts/{postingId}/close", 1L, 10L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(5011));
        }
    }

    @Nested
    @DisplayName("POST /recruitment-posts/{postingId}/view — 공고 조회수 증가")
    class IncrementViewCount {

        @Test
        @DisplayName("성공: 인증 없이 조회수 증가")
        void success() throws Exception {
            // when & then
            mockMvc.perform(post("/recruitment-posts/{postingId}/view", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));

            verify(recruitmentService).incrementViewCount(10L);
        }

        @Test
        @DisplayName("실패: 서비스에서 POSTING_NOT_FOUND → 404, code=5007")
        void failWithPostingNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND))
                    .given(recruitmentService).incrementViewCount(999L);

            // when & then
            mockMvc.perform(post("/recruitment-posts/{postingId}/view", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5007));
        }
    }

    @Nested
    @DisplayName("DELETE /clubs/{clubId}/recruitment-posts/{postingId} — 공고 삭제")
    class DeleteRecruitment {

        @Test
        @DisplayName("성공: 공고 삭제")
        void success() throws Exception {
            // given
            RecruitmentPosting posting = createMockPosting(10L, 1L);
            given(recruitmentService.deleteRecruitment(1L, 1L, 10L)).willReturn(posting);

            // when & then
            mockMvc.perform(delete("/clubs/{clubId}/recruitment-posts/{postingId}", 1L, 10L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.postingId").value(10))
                    .andExpect(jsonPath("$.result.version").value(1));

            verify(recruitmentService).deleteRecruitment(1L, 1L, 10L);
        }

        @Test
        @DisplayName("실패: 서비스에서 POSTING_NOT_FOUND → 404, code=5007")
        void failWithPostingNotFound() throws Exception {
            // given
            willThrow(new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND))
                    .given(recruitmentService).deleteRecruitment(1L, 1L, 999L);

            // when & then
            mockMvc.perform(delete("/clubs/{clubId}/recruitment-posts/{postingId}", 1L, 999L)
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(5007));
        }
    }

    // ── Helper Methods ──

    private RecruitmentPosting createMockPosting(Long id, Long version) {
        RecruitmentPosting posting = RecruitmentPosting.builder()
                .title("테스트 공고")
                .recentRecruitmentVersion(version)
                .startAt(java.time.OffsetDateTime.now())
                .endAt(java.time.OffsetDateTime.now().plusDays(30))
                .editWindowBasis(com.dewple.common.enums.EditWindowBasis.SUBMITTED)
                .editWindowDays(0)
                .recruitmentStatus(com.dewple.common.enums.RecruitmentStatus.OPEN)
                .isInterviewRequired(false)
                .build();
        ReflectionTestUtils.setField(posting, "id", id);
        return posting;
    }

    private Map<String, Object> createValidRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", "2026년 1기 신입 부원 모집");
        request.put("generation", 1);
        request.put("content", List.of(
                Map.of("orderNumber", 1, "text", "안녕하세요, 듀플 동아리입니다.")
        ));
        request.put("departments", List.of(
                Map.of("id", 1L, "count", 5)
        ));
        request.put("startDate", "2026-03-01");
        request.put("endDate", "2026-03-15");
        request.put("resultDate", "2026-03-20");
        request.put("endOfGenerationDate", "2026-12-31");
        request.put("isInterviewRequired", false);
        request.put("applicationForm", createValidApplicationForm());
        return request;
    }

    private Map<String, Object> createValidUpdateRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", "수정된 공고 제목");
        request.put("content", List.of(
                Map.of("orderNumber", 1, "text", "수정된 공고 내용입니다.")
        ));
        request.put("applicationForm", createValidApplicationForm());
        return request;
    }

    private Map<String, Object> createValidApplicationForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("textarea", List.of(
                Map.of(
                        "orderNumber", 1,
                        "key", "q1",
                        "question", "자기소개를 해주세요.",
                        "required", true
                )
        ));
        form.put("choice", List.of());
        form.put("file", List.of());
        form.put("calendar", List.of());
        form.put("when2meet", List.of());
        return form;
    }
}
