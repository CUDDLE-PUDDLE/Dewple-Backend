package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "인증 코드 확인 요청")
public record ConfirmVerificationCodeRequest(
        @Schema(description = "인증 코드 (영숫자 대문자 6자리)", example = "A1B2C3")
        @NotBlank(message = "인증 코드는 필수입니다.")
        @Pattern(regexp = "^[A-Z0-9]{6}$", message = "인증 코드 형식이 올바르지 않습니다.")
        String code
) {
}
