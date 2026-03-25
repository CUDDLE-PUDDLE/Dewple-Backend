package com.dewple.app_api_auth.api.organization.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;
import com.dewple.organization.service.GetOrganizationListParam;
import com.dewple.organization.service.OrganizationService;
import com.dewple.organization.service.OrganizationSummaryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
@Import(SecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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
