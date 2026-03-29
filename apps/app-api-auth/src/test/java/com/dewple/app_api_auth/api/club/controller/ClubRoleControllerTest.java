package com.dewple.app_api_auth.api.club.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.service.ClubRoleResult;
import com.dewple.club.service.ClubRoleService;
import com.dewple.club.service.CreateClubRoleParam;
import com.dewple.club.service.UpdateClubRoleParam;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubRoleController.class)
@Import(SecurityConfig.class)
class ClubRoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ClubRoleService clubRoleService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("POST /clubs/{clubId}/roles - 역할 생성")
    class CreateRole {

        @Test
        @DisplayName("성공: 역할 생성")
        void success() throws Exception {
            ClubRoleResult result = new ClubRoleResult(
                    10L, "홍보담당", List.of("MANAGE_NOTICE", "MANAGE_FEED"), true, false);
            given(clubRoleService.createRole(eq(1L), eq(100L), any(CreateClubRoleParam.class)))
                    .willReturn(result);

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "홍보담당",
                    "permissions", List.of("MANAGE_NOTICE", "MANAGE_FEED"),
                    "isStaff", true
            ));

            mockMvc.perform(post("/clubs/100/roles")
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

            mockMvc.perform(post("/clubs/100/roles")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 없음")
        void failNoAuth() throws Exception {
            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "역할",
                    "permissions", List.of(),
                    "isStaff", false
            ));

            mockMvc.perform(post("/clubs/100/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 권한 없는 유저")
        void failForbidden() throws Exception {
            given(clubRoleService.createRole(eq(999L), eq(100L), any(CreateClubRoleParam.class)))
                    .willThrow(new BusinessException(ClubErrorCode.ROLE_MANAGE_FORBIDDEN));

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "역할",
                    "permissions", List.of(),
                    "isStaff", false
            ));

            mockMvc.perform(post("/clubs/100/roles")
                            .with(jwt().jwt(j -> j.subject("999")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/roles/{roleId} - 역할 수정")
    class UpdateRole {

        @Test
        @DisplayName("성공: 역할 수정")
        void success() throws Exception {
            ClubRoleResult result = new ClubRoleResult(
                    10L, "수정된역할", List.of("MANAGE_CALENDAR"), false, false);
            given(clubRoleService.updateRole(eq(1L), eq(100L), eq(10L), any(UpdateClubRoleParam.class)))
                    .willReturn(result);

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "수정된역할",
                    "permissions", List.of("MANAGE_CALENDAR"),
                    "isStaff", false
            ));

            mockMvc.perform(patch("/clubs/100/roles/10")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.name").value("수정된역할"))
                    .andExpect(jsonPath("$.result.permissions[0]").value("MANAGE_CALENDAR"));
        }

        @Test
        @DisplayName("실패: 회장 역할 수정 시도")
        void failPresidentRole() throws Exception {
            given(clubRoleService.updateRole(eq(1L), eq(100L), eq(1L), any(UpdateClubRoleParam.class)))
                    .willThrow(new BusinessException(ClubErrorCode.PRESIDENT_ROLE_NOT_MODIFIABLE));

            String json = objectMapper.writeValueAsString(java.util.Map.of(
                    "name", "변경", "permissions", List.of(), "isStaff", false));

            mockMvc.perform(patch("/clubs/100/roles/1")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /clubs/{clubId}/roles/{roleId} - 역할 삭제")
    class DeleteRole {

        @Test
        @DisplayName("성공: 커스텀 역할 삭제")
        void success() throws Exception {
            willDoNothing().given(clubRoleService).deleteRole(eq(1L), eq(100L), eq(10L));

            mockMvc.perform(delete("/clubs/100/roles/10")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 기본 역할 삭제 시도")
        void failDefaultRole() throws Exception {
            willThrow(new BusinessException(ClubErrorCode.ROLE_DEFAULT_NOT_DELETABLE))
                    .given(clubRoleService).deleteRole(eq(1L), eq(100L), eq(1L));

            mockMvc.perform(delete("/clubs/100/roles/1")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /clubs/{clubId}/roles/members/{memberId} - 멤버에 역할 부여")
    class AssignRole {

        @Test
        @DisplayName("성공: 멤버에 역할 부여")
        void success() throws Exception {
            willDoNothing().given(clubRoleService)
                    .assignRole(eq(1L), eq(100L), eq(60L), eq(10L));

            mockMvc.perform(patch("/clubs/100/roles/members/60")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("roleId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000));
        }

        @Test
        @DisplayName("실패: 회장 역할 직접 할당")
        void failPresidentRole() throws Exception {
            willThrow(new BusinessException(ClubErrorCode.PRESIDENT_ROLE_NOT_ASSIGNABLE))
                    .given(clubRoleService).assignRole(eq(1L), eq(100L), eq(60L), eq(1L));

            mockMvc.perform(patch("/clubs/100/roles/members/60")
                            .with(jwt().jwt(j -> j.subject("1")))
                            .param("roleId", "1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 인증 없음")
        void failNoAuth() throws Exception {
            mockMvc.perform(patch("/clubs/100/roles/members/60")
                            .param("roleId", "10"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /clubs/{clubId}/roles - 역할 목록 조회")
    class GetRoles {

        @Test
        @DisplayName("성공: 역할 목록 조회")
        void success() throws Exception {
            List<ClubRoleResult> results = List.of(
                    new ClubRoleResult(1L, "회장", List.of("PROPOSE_ACTIVITY", "MANAGE_ACTIVITY"), true, true),
                    new ClubRoleResult(10L, "홍보담당", List.of("MANAGE_NOTICE"), true, false)
            );
            given(clubRoleService.getRoles(eq(100L))).willReturn(results);

            mockMvc.perform(get("/clubs/100/roles")
                            .with(jwt().jwt(j -> j.subject("1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].name").value("회장"))
                    .andExpect(jsonPath("$.result[0].isDefault").value(true))
                    .andExpect(jsonPath("$.result[1].name").value("홍보담당"));
        }
    }
}
