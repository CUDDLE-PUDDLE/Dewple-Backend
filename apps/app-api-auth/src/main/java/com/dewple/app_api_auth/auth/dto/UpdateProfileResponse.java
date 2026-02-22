package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 설정 응답")
public record UpdateProfileResponse(
        @Schema(description = "사용자 아이디")
        String userId,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "이메일")
        String email
) {
}
