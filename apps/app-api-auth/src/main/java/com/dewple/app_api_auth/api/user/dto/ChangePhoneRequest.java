package com.dewple.app_api_auth.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "전화번호 변경 요청")
public record ChangePhoneRequest(
        @Schema(description = "전화번호 인증 토큰", example = "vp_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
        @NotBlank(message = "인증 토큰은 필수입니다.")
        String verificationToken
) {
}
