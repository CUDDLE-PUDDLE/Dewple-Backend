package com.dewple.app_api_auth.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "회원 프로필 조회 응답")
public record GetUserProfileResponse(
        @Schema(description = "이름")
        String name,

        @Schema(description = "프로필 이미지 URL")
        String profileImg,

        @Schema(description = "한줄소개")
        String selfIntroduction,

        @Schema(description = "MBTI")
        String mbti,

        @Schema(description = "관심 분야 목록")
        List<String> interests,

        @Schema(description = "별점 (null이면 평가 없음)", example = "4.2")
        BigDecimal star
) {
}
