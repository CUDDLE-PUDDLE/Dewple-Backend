package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.common.enums.ReportCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모임 신고 요청")
public record CreateReportRequest(
        @Schema(description = "신고 카테고리", example = "INAPPROPRIATE_CONTENT")
        @NotNull(message = "신고 카테고리는 필수입니다.")
        ReportCategory category,

        @Schema(description = "신고 사유")
        @NotBlank(message = "신고 사유는 필수입니다.")
        String reason
) {
}
