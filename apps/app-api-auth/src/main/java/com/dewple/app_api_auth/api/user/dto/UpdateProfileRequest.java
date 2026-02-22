package com.dewple.app_api_auth.api.user.dto;

import com.dewple.common.enums.Gender;
import com.dewple.common.enums.University;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "프로필 설정 요청")
public record UpdateProfileRequest(
        @Schema(description = "닉네임", example = "듀플러")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 이내여야 합니다.")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "생년월일", example = "2000-01-01")
        LocalDate birthdate,

        @Schema(description = "성별", example = "MALE")
        Gender gender,

        @Schema(description = "대학교", example = "SEOUL_NATIONAL")
        University university,

        @Schema(description = "졸업 여부", example = "false")
        Boolean isGraduated,

        @Schema(description = "직장", example = "듀플 주식회사")
        @Size(max = 100, message = "직장명은 100자 이내여야 합니다.")
        String workplace
) {
}
