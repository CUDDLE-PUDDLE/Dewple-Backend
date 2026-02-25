package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 코드 확인 응답")
public record ConfirmVerificationCodeResponse(
        @Schema(description = "본인인증 완료 토큰 (회원가입 시 사용)")
        String verificationToken
) {
}
