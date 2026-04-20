package com.dewple.app_api_auth.api.club.dto;

import com.dewple.club.service.ClubSummaryResult;
import com.dewple.common.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동아리 목록 조회 응답")
public record GetClubListResponse(
        @Schema(description = "동아리 ID")
        Long id,
        @Schema(description = "동아리 이름")
        String name,
        @Schema(description = "커버 이미지 URL")
        String coverImg,
        @Schema(description = "활동 방식")
        ActivityType activityType,
        @Schema(description = "본인인증 필수 여부")
        Boolean isVerificationRequired,
        @Schema(description = "좋아요 수")
        Integer likeCount,
        @Schema(description = "부원 수")
        Long memberCount
) {
    public static GetClubListResponse from(ClubSummaryResult result) {
        return new GetClubListResponse(
                result.id(), result.name(), result.coverImg(),
                result.activityType(), result.isVerificationRequired(),
                result.likeCount(), result.memberCount()
        );
    }
}
