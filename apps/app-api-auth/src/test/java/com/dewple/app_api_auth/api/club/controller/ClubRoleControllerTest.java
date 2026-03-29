package com.dewple.app_api_auth.api.club.controller;

import com.dewple.app_api_auth.global.config.SecurityConfig;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.service.ClubRoleResult;
import com.dewple.club.service.ClubRoleService;
import com.dewple.club.service.CreateClubRoleParam;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
