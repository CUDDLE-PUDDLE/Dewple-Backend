package com.dewple.app_api_auth.api.organization.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.exception.OrganizationErrorCode;
import com.dewple.organization.service.GetOrganizationListParam;
import com.dewple.organization.service.OrganizationDetailResult;
import com.dewple.organization.service.OrganizationService;
import com.dewple.organization.service.OrganizationSummaryResult;
import com.dewple.organization.service.UpdateOrganizationParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
@Import(SecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("PATCH /organizations/{organizationId} - 연합회 정보 수정")
    class UpdateOrganization {

        private String createUpdateRequestJson() throws Exception {
            return objectMapper.writeValueAsString(java.util.Map.ofEntries(
                    java.util.Map.entry("name", "수정된 연합회"),
                    java.util.Map.entry("description", "수정된 설명"),
                    java.util.Map.entry("coverImg", "new-cover.jpg"),
                    java.util.Map.entry("type", "ENTERPRISE"),
                    java.util.Map.entry("activityType", "ONLINE"),
                    java.util.Map.entry("purpose", "수정된 목적"),
                    java.util.Map.entry("contactEmail", "new@email.com"),
                    java.util.Map.entry("contactPhone", "01099999999"),
                    java.util.Map.entry("contactPreference", "PHONE"),
                    java.util.Map.entry("targetClubsDescription", "수정된 대상"),
                    java.util.Map.entry("categoryIds", List.of(2, 3)),
                    java.util.Map.entry("regionIds", List.of(2))
            ));
        }

        @Test
        @DisplayName("성공: 연합회 정보 수정")
        void success() throws Exception {
            // given
            willDoNothing().given(organizationService)
                    .updateOrganization(eq(1L), eq(100L), any(UpdateOrganizationParam.class));

            // when & then
            mockMvc.perform(patch("/organizations/100")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUpdateRequestJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 인증 없이 수정 시도")
        void failWithoutAuth() throws Exception {
            mockMvc.perform(patch("/organizations/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUpdateRequestJson()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 권한 없는 유저의 수정 시도")
        void failForbidden() throws Exception {
            // given
            willThrow(new BusinessException(OrganizationErrorCode.ORGANIZATION_UPDATE_FORBIDDEN))
                    .given(organizationService)
                    .updateOrganization(eq(999L), eq(100L), any(UpdateOrganizationParam.class));

            // when & then
            mockMvc.perform(patch("/organizations/100")
                            .with(jwt().jwt(j -> j.subject("999")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUpdateRequestJson()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 승인되지 않은 연합회 수정")
        void failNotApproved() throws Exception {
            // given
            willThrow(new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED))
                    .given(organizationService)
                    .updateOrganization(eq(1L), eq(100L), any(UpdateOrganizationParam.class));

            // when & then
            mockMvc.perform(patch("/organizations/100")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUpdateRequestJson()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 이름 누락 (Validation)")
        void failNameBlank() throws Exception {
            String json = objectMapper.writeValueAsString(java.util.Map.ofEntries(
                    java.util.Map.entry("name", ""),
                    java.util.Map.entry("type", "UNIVERSITY"),
                    java.util.Map.entry("activityType", "BOTH"),
                    java.util.Map.entry("purpose", "목적"),
                    java.util.Map.entry("contactEmail", "a@b.com"),
                    java.util.Map.entry("contactPhone", "010"),
                    java.util.Map.entry("contactPreference", "EMAIL"),
                    java.util.Map.entry("categoryIds", List.of(1)),
                    java.util.Map.entry("regionIds", List.of(1))
            ));

            mockMvc.perform(patch("/organizations/100")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /organizations/{organizationId} - 연합회 단건 조회")
    class GetOrganizationDetail {

        @Test
        @DisplayName("성공: 승인된 연합회 상세 조회")
        void success() throws Exception {
            // given
            OrganizationDetailResult result = new OrganizationDetailResult(
                    1L, "서울대 연합회", "서울대학교 동아리 연합",
                    "cover.jpg", "{\"components\":[]}",
                    OrganizationType.UNIVERSITY, ActivityType.BOTH,
                    LocalDate.of(2020, 3, 1), "동아리 관리 및 지원",
                    "[1, 2]", "[1]", 10L
            );

            given(organizationService.getOrganizationDetail(eq(1L))).willReturn(result);

            // when & then
            mockMvc.perform(get("/organizations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.id").value(1))
                    .andExpect(jsonPath("$.result.name").value("서울대 연합회"))
                    .andExpect(jsonPath("$.result.description").value("서울대학교 동아리 연합"))
                    .andExpect(jsonPath("$.result.coverImg").value("cover.jpg"))
                    .andExpect(jsonPath("$.result.landingPage").value("{\"components\":[]}"))
                    .andExpect(jsonPath("$.result.type").value("UNIVERSITY"))
                    .andExpect(jsonPath("$.result.activityType").value("BOTH"))
                    .andExpect(jsonPath("$.result.foundedDate").value("2020-03-01"))
                    .andExpect(jsonPath("$.result.purpose").value("동아리 관리 및 지원"))
                    .andExpect(jsonPath("$.result.categoryIds").value("[1, 2]"))
                    .andExpect(jsonPath("$.result.regionIds").value("[1]"))
                    .andExpect(jsonPath("$.result.creatorId").value(10));
        }

        @Test
        @DisplayName("성공: 소개페이지/설명 없는 연합회 조회")
        void successWithNullableFields() throws Exception {
            // given
            OrganizationDetailResult result = new OrganizationDetailResult(
                    2L, "기업 연합회", null,
                    null, null,
                    OrganizationType.ENTERPRISE, ActivityType.OFFLINE,
                    null, "기업 연합 운영",
                    "[3]", "[2]", 20L
            );

            given(organizationService.getOrganizationDetail(eq(2L))).willReturn(result);

            // when & then
            mockMvc.perform(get("/organizations/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.id").value(2))
                    .andExpect(jsonPath("$.result.description").doesNotExist())
                    .andExpect(jsonPath("$.result.coverImg").doesNotExist())
                    .andExpect(jsonPath("$.result.landingPage").doesNotExist())
                    .andExpect(jsonPath("$.result.foundedDate").doesNotExist());
        }

        @Test
        @DisplayName("실패: 승인되지 않은 연합회 조회")
        void failNotApproved() throws Exception {
            // given
            given(organizationService.getOrganizationDetail(eq(1L)))
                    .willThrow(new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED));

            // when & then
            mockMvc.perform(get("/organizations/1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 연합회 조회")
        void failNotFound() throws Exception {
            // given
            given(organizationService.getOrganizationDetail(eq(999L)))
                    .willThrow(new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/organizations/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("성공: 인증 없이 조회 가능 (공개 API)")
        void successWithoutAuthentication() throws Exception {
            // given
            OrganizationDetailResult result = new OrganizationDetailResult(
                    1L, "연합회", null, null, null,
                    OrganizationType.OTHER, ActivityType.BOTH,
                    null, "목적", "[1]", "[1]", 10L
            );

            given(organizationService.getOrganizationDetail(eq(1L))).willReturn(result);

            // when & then (no jwt())
            mockMvc.perform(get("/organizations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }
    }

    @Nested
    @DisplayName("GET /organizations - 연합회 목록 조회")
    class GetOrganizationList {

        @Test
        @DisplayName("성공: 필터 없이 목록 조회")
        void successWithoutFilters() throws Exception {
            // given
            List<OrganizationSummaryResult> content = List.of(
                    new OrganizationSummaryResult(1L, "서울대 연합회", null,
                            OrganizationType.UNIVERSITY, ActivityType.BOTH, "[1, 2]", "[1]"),
                    new OrganizationSummaryResult(2L, "기업 연합회", "cover.jpg",
                            OrganizationType.ENTERPRISE, ActivityType.OFFLINE, "[3]", "[2]")
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            given(organizationService.getOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when & then
            mockMvc.perform(get("/organizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content.length()").value(2))
                    .andExpect(jsonPath("$.result.content[0].id").value(1))
                    .andExpect(jsonPath("$.result.content[0].name").value("서울대 연합회"))
                    .andExpect(jsonPath("$.result.content[0].type").value("UNIVERSITY"))
                    .andExpect(jsonPath("$.result.content[0].activityType").value("BOTH"))
                    .andExpect(jsonPath("$.result.content[1].id").value(2))
                    .andExpect(jsonPath("$.result.content[1].coverImg").value("cover.jpg"))
                    .andExpect(jsonPath("$.result.hasNext").value(false));
        }

        @Test
        @DisplayName("성공: 종류 필터로 조회")
        void successWithTypeFilter() throws Exception {
            // given
            List<OrganizationSummaryResult> content = List.of(
                    new OrganizationSummaryResult(1L, "서울대 연합회", null,
                            OrganizationType.UNIVERSITY, ActivityType.BOTH, "[1]", "[1]")
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            given(organizationService.getOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when & then
            mockMvc.perform(get("/organizations")
                            .param("type", "UNIVERSITY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content.length()").value(1))
                    .andExpect(jsonPath("$.result.content[0].type").value("UNIVERSITY"));
        }

        @Test
        @DisplayName("성공: 활동 방식 필터로 조회")
        void successWithActivityTypeFilter() throws Exception {
            // given
            var slice = new SliceImpl<>(
                    List.<OrganizationSummaryResult>of(), PageRequest.of(0, 10), false
            );

            given(organizationService.getOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when & then
            mockMvc.perform(get("/organizations")
                            .param("activityType", "ONLINE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content").isEmpty());
        }

        @Test
        @DisplayName("성공: 다중 필터 조합 조회")
        void successWithMultipleFilters() throws Exception {
            // given
            List<OrganizationSummaryResult> content = List.of(
                    new OrganizationSummaryResult(1L, "대학 연합회", null,
                            OrganizationType.UNIVERSITY, ActivityType.OFFLINE, "[1]", "[3]")
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);

            given(organizationService.getOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when & then
            mockMvc.perform(get("/organizations")
                            .param("categoryId", "1")
                            .param("regionId", "3")
                            .param("activityType", "OFFLINE")
                            .param("type", "UNIVERSITY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.content.length()").value(1));
        }

        @Test
        @DisplayName("성공: 페이징 - 다음 페이지 존재")
        void successWithNextPage() throws Exception {
            // given
            List<OrganizationSummaryResult> content = List.of(
                    new OrganizationSummaryResult(1L, "연합회1", null,
                            OrganizationType.OTHER, ActivityType.BOTH, "[1]", "[1]")
            );
            var slice = new SliceImpl<>(content, PageRequest.of(0, 1), true);

            given(organizationService.getOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when & then
            mockMvc.perform(get("/organizations")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content.length()").value(1))
                    .andExpect(jsonPath("$.result.hasNext").value(true))
                    .andExpect(jsonPath("$.result.size").value(1));
        }

        @Test
        @DisplayName("성공: 인증 없이 조회 가능 (공개 API)")
        void successWithoutAuthentication() throws Exception {
            // given
            var slice = new SliceImpl<>(
                    List.<OrganizationSummaryResult>of(), PageRequest.of(0, 10), false
            );

            given(organizationService.getOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            // when & then (no jwt() — still accessible)
            mockMvc.perform(get("/organizations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }
    }
}
