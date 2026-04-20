package com.dewple.app_api_auth.api.club.dto;

import com.dewple.club.service.ClubDetailResult;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "동아리 단건 조회 응답")
public record GetClubDetailResponse(
        @Schema(description = "동아리 ID")
        Long id,
        @Schema(description = "동아리 이름")
        String name,
        @Schema(description = "동아리 설명")
        String description,
        @Schema(description = "커버 이미지 URL")
        String coverImg,
        @Schema(description = "소개페이지 (JSON)")
        String landingPage,
        @Schema(description = "활동 방식")
        ActivityType activityType,
        @Schema(description = "본인인증 필수 여부")
        Boolean isVerificationRequired,
        @Schema(description = "성별 제한")
        Gender gender,
        @Schema(description = "최소 연령")
        Long minAge,
        @Schema(description = "최대 연령")
        Long maxAge,
        @Schema(description = "설립일")
        LocalDate foundedDate,
        @Schema(description = "좋아요 수")
        Integer likeCount,
        @Schema(description = "평판 점수")
        BigDecimal reputationScore,
        @Schema(description = "생성자 ID")
        Long creatorId
) {
    public static GetClubDetailResponse from(ClubDetailResult result) {
        return new GetClubDetailResponse(
                result.id(), result.name(), result.description(),
                result.coverImg(), result.landingPage(),
                result.activityType(), result.isVerificationRequired(),
                result.gender(), result.minAge(), result.maxAge(),
                result.foundedDate(), result.likeCount(),
                result.reputationScore(), result.creatorId()
        );
    }
}
