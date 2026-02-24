package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "SMS 인증 코드 발송 응답")
public record SendVerificationCodeResponse(
        @Schema(description = "인증 요청 ID")
        String verificationId,

        @Schema(description = "인증 코드 만료 시각")
        OffsetDateTime expiredAt,

        @Schema(description = "재발송 가능까지 남은 시간 (초)", example = "60")
        int resendAfterSeconds
) {
}
