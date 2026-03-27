package com.dewple.app_api_auth.api.club.dto;

import com.dewple.common.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "동아리 생성 요청")
public record CreateClubRequest(
        @Schema(description = "동아리 이름", example = "코딩 동아리")
        @NotBlank(message = "동아리 이름은 필수입니다.")
        @Size(max = 50, message = "동아리 이름은 50자 이내여야 합니다.")
        String name,

        @Schema(description = "본인인증 필수 여부", example = "false")
        @NotNull(message = "본인인증 필수 여부는 필수입니다.")
        Boolean isVerificationRequired,

        @Schema(description = "활동 방식", example = "BOTH")
        @NotNull(message = "활동 방식은 필수입니다.")
        ActivityType activityType,

        @Schema(description = "설립일")
        LocalDate foundedDate,

        @Schema(description = "카테고리 ID 목록 (1~3개)", example = "[1, 2]")
        @NotNull(message = "카테고리는 필수입니다.")
        @Size(min = 1, max = 3, message = "카테고리는 1~3개 선택 가능합니다.")
        List<Long> categoryIds,

        @Schema(description = "지역 ID 목록", example = "[1]")
        @NotNull(message = "지역은 필수입니다.")
        @Size(min = 1, message = "지역은 최소 1개 필수입니다.")
        List<Long> regionIds
) {
}
