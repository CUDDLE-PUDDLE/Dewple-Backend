package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.common.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "내 지원 내역 응답")
public record MyApplicationListResponse(
        @Schema(description = "지원서 ID")
        Long applicationId,

        @Schema(description = "공고 ID")
        Long postingId,

        @Schema(description = "동아리 ID")
        Long clubId,

        @Schema(description = "동아리 이름")
        String clubName,

        @Schema(description = "공고 제목")
        String postingTitle,

        @Schema(description = "지원서 상태")
        ApplicationStatus applicationStatus,

        @Schema(description = "지원 일시")
        OffsetDateTime createdAt,

        @Schema(description = "최종 수정 일시")
        OffsetDateTime updatedAt
) {
}
