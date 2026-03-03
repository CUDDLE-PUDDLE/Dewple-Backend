package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.OpenType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "모임 생성 요청")
public record CreateActivityRequest(

        @Schema(description = "동아리 ID (개인 모임이면 null)", example = "1")
        Long clubId,

        @Schema(description = "공개 타입", example = "PUBLIC")
        @NotNull(message = "공개 타입은 필수입니다.")
        OpenType openType,

        @Schema(description = "모임 이름", example = "봄맞이 독서 모임")
        @NotBlank(message = "모임 이름은 필수입니다.")
        @Size(max = 100, message = "모임 이름은 100자 이내여야 합니다.")
        String name,

        @Schema(description = "모임 설명", example = "함께 책을 읽고 토론하는 모임입니다.")
        @NotBlank(message = "모임 설명은 필수입니다.")
        String description,

        @Schema(description = "최대 모집 인원 (null이면 제한 없음)", example = "20")
        @Positive(message = "최대 모집 인원은 1 이상이어야 합니다.")
        Integer capacity,

        @Schema(description = "출석 체크 여부", example = "false")
        Boolean isAttendanceCheck,

        @Schema(description = "검색 노출 여부 (PRIVATE 모임에서만 의미)", example = "true")
        Boolean isSearchable,

        @Schema(description = "시작 시간", example = "2026-04-01T10:00:00+09:00")
        @NotNull(message = "시작 시간은 필수입니다.")
        OffsetDateTime startAt,

        @Schema(description = "종료 시간", example = "2026-04-01T12:00:00+09:00")
        @NotNull(message = "종료 시간은 필수입니다.")
        OffsetDateTime endAt,

        @Schema(description = "카테고리 ID", example = "1")
        Long categoryId,

        @Schema(description = "지역 ID", example = "1")
        Long regionId,

        @Schema(description = "활동 방식 (ONLINE, OFFLINE, BOTH)", example = "BOTH")
        @NotNull(message = "활동 방식은 필수입니다.")
        ActivityType activityType,

        @Schema(description = "본인인증 필수 여부", example = "false")
        Boolean isVerificationRequired,

        @Schema(description = "최소 연령", example = "20")
        Integer minAge,

        @Schema(description = "최대 연령", example = "30")
        Integer maxAge,

        @Schema(description = "성별 제한 (MALE, FEMALE, ANY)", example = "ANY")
        Gender gender
) {
}
