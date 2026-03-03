package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.common.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "지원자 목록 응답")
public record ApplicationListResponse(
        @Schema(description = "지원서 ID")
        Long applicationId,

        @Schema(description = "지원자 이름")
        String applicantName,

        @Schema(description = "지원자 연락처")
        String applicantPhone,

        @Schema(description = "지원 일시")
        OffsetDateTime appliedAt,

        @Schema(description = "지원서 상태")
        ApplicationStatus applicationStatus
) {
}
