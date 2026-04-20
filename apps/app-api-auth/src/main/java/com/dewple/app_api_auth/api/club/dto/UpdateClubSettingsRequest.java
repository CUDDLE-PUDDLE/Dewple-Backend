package com.dewple.app_api_auth.api.club.dto;

import com.dewple.common.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "동아리 설정 변경 요청")
public record UpdateClubSettingsRequest(
        @Schema(description = "본인인증 필수 여부", example = "true")
        @NotNull(message = "본인인증 필수 여부는 필수입니다.")
        Boolean isVerificationRequired,

        @Schema(description = "성별 (본인인증 필수 시만 설정 가능)", example = "ANY")
        Gender gender,

        @Schema(description = "최소 연령 (본인인증 필수 시만 설정 가능)", example = "20")
        @Min(value = 0, message = "최소 연령은 0 이상이어야 합니다.")
        @Max(value = 100, message = "최소 연령은 100 이하여야 합니다.")
        Long minAge,

        @Schema(description = "최대 연령 (본인인증 필수 시만 설정 가능)", example = "30")
        @Min(value = 0, message = "최대 연령은 0 이상이어야 합니다.")
        @Max(value = 100, message = "최대 연령은 100 이하여야 합니다.")
        Long maxAge
) {
}
