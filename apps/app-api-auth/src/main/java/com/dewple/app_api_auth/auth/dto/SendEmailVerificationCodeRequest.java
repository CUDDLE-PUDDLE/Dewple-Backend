package com.dewple.app_api_auth.auth.dto;

import com.dewple.common.enums.VerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이메일 인증 코드 발송 요청")
public record SendEmailVerificationCodeRequest(
        @Schema(description = "이메일 주소", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "인증 목적", example = "SIGN_UP")
        @NotNull(message = "인증 목적은 필수입니다.")
        VerificationPurpose purpose
) {
}
