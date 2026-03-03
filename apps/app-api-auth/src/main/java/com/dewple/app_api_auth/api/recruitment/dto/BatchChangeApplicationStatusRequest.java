package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.common.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "지원서 일괄 상태 변경 요청")
public record BatchChangeApplicationStatusRequest(
        @Schema(description = "지원서 ID 목록")
        @NotEmpty
        List<Long> applicationIds,

        @Schema(description = "변경할 상태", example = "ACCEPTED")
        @NotNull
        ApplicationStatus applicationStatus
) {
}
