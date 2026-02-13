package com.dewple.app_api_auth.auth.dto;

import com.dewple.common.enums.VerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "SMS 인증 코드 발송 요청")
public record SendVerificationCodeRequest(
        @Schema(description = "전화번호 (하이픈 포함)", example = "010-1234-5678")
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "유효한 전화번호 형식이 아닙니다.")
        String phoneNumber,

        @Schema(description = "인증 목적", example = "SIGN_UP")
        @NotNull(message = "인증 목적은 필수입니다.")
        VerificationPurpose purpose
) {
}
