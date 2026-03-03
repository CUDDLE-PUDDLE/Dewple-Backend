package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.activity.service.ActivitySummaryResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetActivityListResponse(
        @Schema(description = "모임 ID") Long activityId,
        @Schema(description = "썸네일 URL") String thumbnailUrl,
        @Schema(description = "모임 타입 (PERSONAL / CLUB)") String activityType,
        @Schema(description = "동아리 이름 (사설모임이면 null)") String clubName,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "카테고리 이름") String categoryName,
        @Schema(description = "지역 이름") String regionName,
        @Schema(description = "참가 인원") int participantCount,
        @Schema(description = "정원 (null이면 무제한)") Integer capacity,
        @Schema(description = "좋아요 수") int likeCount,
        @Schema(description = "조회수") int viewCount,
        @Schema(description = "댓글 수") int commentCount,
        @Schema(description = "좋아요 여부") boolean isLiked
) {
    public static GetActivityListResponse from(ActivitySummaryResult result) {
        return new GetActivityListResponse(
                result.activityId(),
                result.thumbnailUrl(),
                result.activityType(),
                result.clubName(),
                result.name(),
                result.categoryName(),
                result.regionName(),
                result.participantCount(),
                result.capacity(),
                result.likeCount(),
                result.viewCount(),
                result.commentCount(),
                result.isLiked()
        );
    }
}
