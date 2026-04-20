package com.dewple.app_api_auth.api.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "연합회 생성 반려 요청")
public record RejectOrganizationRequest(
        @Schema(description = "반려 사유")
        @NotBlank(message = "반려 사유는 필수입니다.")
        @Size(max = 500, message = "반려 사유는 500자 이내여야 합니다.")
        String reason
) {
}
