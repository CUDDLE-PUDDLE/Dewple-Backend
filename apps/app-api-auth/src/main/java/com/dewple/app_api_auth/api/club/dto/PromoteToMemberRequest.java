package com.dewple.app_api_auth.api.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "GUEST→MEMBER 승격 요청")
public record PromoteToMemberRequest(
        @Schema(description = "기수 ID", example = "1")
        @NotNull(message = "기수 ID는 필수입니다.")
        Long generationId,

        @Schema(description = "활동 종료일", example = "2026-12-31")
        @NotNull(message = "활동 기한은 필수입니다.")
        LocalDate activityEndDate
) {
}
