package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.common.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지원서 상태 변경 요청")
public record ChangeApplicationStatusRequest(
        @Schema(description = "변경할 상태", example = "ACCEPTED")
        @NotNull
        ApplicationStatus applicationStatus
) {
}
