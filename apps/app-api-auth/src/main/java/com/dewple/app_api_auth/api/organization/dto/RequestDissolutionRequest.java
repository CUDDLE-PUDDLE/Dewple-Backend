package com.dewple.app_api_auth.api.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "연합회 해산 신청 요청")
public record RequestDissolutionRequest(
        @Schema(description = "해산 사유", example = "운영 지속이 어렵습니다.")
        @NotBlank(message = "해산 사유는 필수입니다.")
        @Size(max = 500, message = "해산 사유는 500자 이내여야 합니다.")
        String reason
) {
}
