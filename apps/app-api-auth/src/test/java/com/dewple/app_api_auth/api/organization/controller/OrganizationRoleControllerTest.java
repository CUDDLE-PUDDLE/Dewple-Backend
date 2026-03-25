package com.dewple.app_api_auth.api.organization.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.exception.OrganizationErrorCode;
import com.dewple.organization.service.OrganizationRoleResult;
import com.dewple.organization.service.OrganizationRoleService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationRoleController.class)
@Import(SecurityConfig.class)
class OrganizationRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationRoleService organizationRoleService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("GET /organizations/{id}/roles - 역할 목록 조회")
    class GetRoles {

        @Test
        @DisplayName("성공: 역할 목록 조회")
        void success() throws Exception {
            List<OrganizationRoleResult> results = List.of(
                    new OrganizationRoleResult(1L, "대표",
                            List.of("DELEGATE_REPRESENTATIVE", "MANAGE_ORGANIZATION"), true, true),
                    new OrganizationRoleResult(2L, "연합회원", List.of(), false, true)
            );
            given(organizationRoleService.getRoles(eq(100L))).willReturn(results);

            mockMvc.perform(get("/organizations/100/roles")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].name").value("대표"))
                    .andExpect(jsonPath("$.result[0].isDefault").value(true))
                    .andExpect(jsonPath("$.result[0].permissions[0]").value("DELEGATE_REPRESENTATIVE"))
                    .andExpect(jsonPath("$.result[1].name").value("연합회원"));
        }
    }

    @Nested
    @DisplayName("POST /organizations/{id}/roles - 역할 생성")
    class CreateRole {

        @Test
        @DisplayName("성공: 역할 생성")
        void success() throws Exception {
            OrganizationRoleResult result = new OrganizationRoleResult(
                    10L, "홍보담당", List.of("MANAGE_NOTICE", "MANAGE_FEED"), true, false
            );
            given(organizationRoleService.createRole(eq(1L), eq(100L), any())).willReturn(result);

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "홍보담당",
                    "permissions", List.of("MANAGE_NOTICE", "MANAGE_FEED"),
                    "isStaff", true
            ));

            mockMvc.perform(post("/organizations/100/roles")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.id").value(10))
                    .andExpect(jsonPath("$.result.name").value("홍보담당"))
                    .andExpect(jsonPath("$.result.isStaff").value(true))
                    .andExpect(jsonPath("$.result.isDefault").value(false));
        }

        @Test
        @DisplayName("실패: 이름 누락")
        void failNameBlank() throws Exception {
            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "",
                    "permissions", List.of(),
                    "isStaff", false
            ));

            mockMvc.perform(post("/organizations/100/roles")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 없음")
        void failNoAuth() throws Exception {
            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "역할", "permissions", List.of(), "isStaff", false
            ));

            mockMvc.perform(post("/organizations/100/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /organizations/{id}/roles/{roleId} - 역할 수정")
    class UpdateRole {

        @Test
        @DisplayName("성공: 역할 수정")
        void success() throws Exception {
            OrganizationRoleResult result = new OrganizationRoleResult(
                    10L, "수정된역할", List.of("MANAGE_CALENDAR"), false, false
            );
            given(organizationRoleService.updateRole(eq(1L), eq(100L), eq(10L), any())).willReturn(result);

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "수정된역할",
                    "permissions", List.of("MANAGE_CALENDAR"),
                    "isStaff", false
            ));

            mockMvc.perform(patch("/organizations/100/roles/10")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.name").value("수정된역할"));
        }

        @Test
        @DisplayName("실패: 기본 역할 수정")
        void failDefaultRole() throws Exception {
            willThrow(new BusinessException(OrganizationErrorCode.ROLE_DEFAULT_NOT_MODIFIABLE))
                    .given(organizationRoleService).updateRole(eq(1L), eq(100L), eq(1L), any());

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "변경", "permissions", List.of(), "isStaff", false
            ));

            mockMvc.perform(patch("/organizations/100/roles/1")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /organizations/{id}/roles/{roleId} - 역할 삭제")
    class DeleteRole {

        @Test
        @DisplayName("성공: 역할 삭제")
        void success() throws Exception {
            willDoNothing().given(organizationRoleService).deleteRole(eq(1L), eq(100L), eq(10L));

            mockMvc.perform(delete("/organizations/100/roles/10")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 기본 역할 삭제")
        void failDefaultRole() throws Exception {
            willThrow(new BusinessException(OrganizationErrorCode.ROLE_DEFAULT_NOT_DELETABLE))
                    .given(organizationRoleService).deleteRole(eq(1L), eq(100L), eq(1L));

            mockMvc.perform(delete("/organizations/100/roles/1")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /organizations/{id}/roles/members/{memberId} - 멤버에 역할 부여")
    class AssignRole {

        @Test
        @DisplayName("성공: 멤버에 역할 부여")
        void success() throws Exception {
            willDoNothing().given(organizationRoleService).assignRole(eq(1L), eq(100L), eq(50L), eq(10L));

            mockMvc.perform(patch("/organizations/100/roles/members/50")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("roleId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 대표 역할 직접 할당")
        void failRepresentativeRole() throws Exception {
            willThrow(new BusinessException(OrganizationErrorCode.REPRESENTATIVE_ROLE_NOT_ASSIGNABLE))
                    .given(organizationRoleService).assignRole(eq(1L), eq(100L), eq(50L), eq(1L));

            mockMvc.perform(patch("/organizations/100/roles/members/50")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("roleId", "1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 없음")
        void failNoAuth() throws Exception {
            mockMvc.perform(patch("/organizations/100/roles/members/50")
                            .param("roleId", "10"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /organizations/{id}/roles/delegate/{targetMemberId} - 대표 위임")
    class DelegateRepresentative {

        @Test
        @DisplayName("성공: 대표 위임")
        void success() throws Exception {
            willDoNothing().given(organizationRoleService).delegateRepresentative(eq(1L), eq(100L), eq(51L));

            mockMvc.perform(post("/organizations/100/roles/delegate/51")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 대표가 아닌 유저")
        void failNotRepresentative() throws Exception {
            willThrow(new BusinessException(OrganizationErrorCode.DELEGATE_FORBIDDEN))
                    .given(organizationRoleService).delegateRepresentative(eq(999L), eq(100L), eq(51L));

            mockMvc.perform(post("/organizations/100/roles/delegate/51")
                            .with(jwt().jwt(j -> j.subject("999"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 자기 자신에게 위임")
        void failDelegateSelf() throws Exception {
            willThrow(new BusinessException(OrganizationErrorCode.DELEGATE_SELF))
                    .given(organizationRoleService).delegateRepresentative(eq(1L), eq(100L), eq(50L));

            mockMvc.perform(post("/organizations/100/roles/delegate/50")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isBadRequest());
        }
    }
}
