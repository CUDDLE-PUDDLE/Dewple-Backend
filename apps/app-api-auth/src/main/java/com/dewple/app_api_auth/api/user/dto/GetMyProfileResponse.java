package com.dewple.app_api_auth.api.user.dto;

import com.dewple.common.enums.Gender;
import com.dewple.common.enums.Mbti;
import com.dewple.common.enums.University;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "내 프로필 조회 응답")
public record GetMyProfileResponse(
        @Schema(description = "이름")
        String name,

        @Schema(description = "프로필 이미지 URL")
        String profileImg,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "이메일")
        String email,

        @Schema(description = "전화번호")
        String phone,

        @Schema(description = "생년월일")
        LocalDate birthdate,

        @Schema(description = "성별")
        Gender gender,

        @Schema(description = "대학교")
        University university,

        @Schema(description = "졸업 여부")
        Boolean isGraduated,

        @Schema(description = "직장")
        String workplace,

        @Schema(description = "자기소개")
        String selfIntroduction,

        @Schema(description = "MBTI")
        Mbti mbti,

        @Schema(description = "관심 분야 목록")
        List<String> interests
) {
}
