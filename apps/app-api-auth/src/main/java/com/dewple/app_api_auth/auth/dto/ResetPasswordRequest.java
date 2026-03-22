package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 찾기 요청")
public record ResetPasswordRequest(
        @Schema(description = "전화번호 인증 후 발급받은 본인인증 토큰")
        @NotBlank(message = "본인인증 토큰은 필수입니다.")
        String verificationToken
) {
}
