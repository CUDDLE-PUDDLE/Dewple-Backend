package com.dewple.app_api_auth.api.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "별점 평가 요청")
public record RateParticipantRequest(
        @Schema(description = "평가 대상 유저 ID")
        @NotNull(message = "평가 대상은 필수입니다.")
        Long rateeId,

        @Schema(description = "별점 (0.0~5.0, 0.1 단위)", example = "4.5")
        @NotNull(message = "별점은 필수입니다.")
        @DecimalMin(value = "0.0", message = "별점은 0 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5 이하여야 합니다.")
        BigDecimal score
) {
}
