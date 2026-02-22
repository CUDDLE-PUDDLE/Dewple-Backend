package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record SignupResponse(
        @Schema(description = "사용자 아이디")
        String userId
) {
}
