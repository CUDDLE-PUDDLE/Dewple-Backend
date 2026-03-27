package com.dewple.app_api_auth.api.club.dto;

import com.dewple.club.service.CreateClubResult;
import com.dewple.common.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "동아리 생성 응답")
public record CreateClubResponse(
        @Schema(description = "동아리 ID")
        Long clubId,
        @Schema(description = "동아리 이름")
        String name,
        @Schema(description = "본인인증 필수 여부")
        Boolean isVerificationRequired,
        @Schema(description = "활동 방식")
        ActivityType activityType,
        @Schema(description = "설립일")
        LocalDate foundedDate,
        @Schema(description = "카테고리 ID 목록")
        List<Long> categoryIds,
        @Schema(description = "지역 ID 목록")
        List<Long> regionIds,
        @Schema(description = "생성자 ID")
        Long creatorId
) {
    public static CreateClubResponse from(CreateClubResult result) {
        return new CreateClubResponse(
                result.clubId(), result.name(), result.isVerificationRequired(),
                result.activityType(), result.foundedDate(),
                result.categoryIds(), result.regionIds(), result.creatorId()
        );
    }
}
