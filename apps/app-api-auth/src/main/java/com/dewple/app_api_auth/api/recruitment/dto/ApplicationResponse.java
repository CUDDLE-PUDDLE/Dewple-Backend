package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.common.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원서 응답")
public record ApplicationResponse(
        @Schema(description = "지원서 ID")
        Long applicationId,

        @Schema(description = "지원서 상태")
        ApplicationStatus applicationStatus
) {
}
