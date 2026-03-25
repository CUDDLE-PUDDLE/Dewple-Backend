package com.dewple.app_api_auth.api.organization.dto;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.ContactPreference;
import com.dewple.common.enums.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "연합회 생성 신청 요청")
public record CreateOrganizationRequest(
        @Schema(description = "연합회 이름", example = "서울대학교 동아리 연합회")
        @NotBlank(message = "연합회 이름은 필수입니다.")
        @Size(max = 50, message = "연합회 이름은 50자 이내여야 합니다.")
        String name,

        @Schema(description = "운영 목적")
        @NotBlank(message = "운영 목적은 필수입니다.")
        @Size(max = 1000, message = "운영 목적은 1000자 이내여야 합니다.")
        String purpose,

        @Schema(description = "종류", example = "UNIVERSITY")
        @NotNull(message = "종류는 필수입니다.")
        OrganizationType type,

        @Schema(description = "활동 방식", example = "BOTH")
        @NotNull(message = "활동 방식은 필수입니다.")
        ActivityType activityType,

        @Schema(description = "담당자 이메일", example = "contact@example.com")
        @NotBlank(message = "담당자 이메일은 필수입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String contactEmail,

        @Schema(description = "담당자 전화번호", example = "01012345678")
        @NotBlank(message = "담당자 전화번호는 필수입니다.")
        String contactPhone,

        @Schema(description = "선호 연락 방식", example = "EMAIL")
        @NotNull(message = "선호 연락 방식은 필수입니다.")
        ContactPreference contactPreference,

        @Schema(description = "관리 대상 동아리 설명 (동아리 미가입 시)")
        @Size(max = 500, message = "설명은 500자 이내여야 합니다.")
        String targetClubsDescription,

        @Schema(description = "관리 대상 동아리 ID 목록", example = "[1, 2]")
        List<Long> targetClubIds,

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
