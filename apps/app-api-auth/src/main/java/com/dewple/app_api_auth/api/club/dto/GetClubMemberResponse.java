package com.dewple.app_api_auth.api.club.dto;

import com.dewple.club.service.ClubMemberResult;
import com.dewple.common.enums.ActivityStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "동아리 멤버 응답")
public record GetClubMemberResponse(
        @Schema(description = "멤버 ID")
        Long memberId,
        @Schema(description = "유저 ID")
        Long userId,
        @Schema(description = "이름")
        String name,
        @Schema(description = "닉네임")
        String nickname,
        @Schema(description = "프로필 이미지")
        String profileImg,
        @Schema(description = "역할명")
        String roleName,
        @Schema(description = "활동 상태")
        ActivityStatus activityStatus,
        @Schema(description = "별점")
        BigDecimal reputationScore
) {
    public static GetClubMemberResponse from(ClubMemberResult result) {
        return new GetClubMemberResponse(
                result.memberId(), result.userId(),
                result.name(), result.nickname(), result.profileImg(),
                result.roleName(), result.activityStatus(),
                result.reputationScore()
        );
    }
}
