package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.OpenType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "모임 생성 응답")
public record CreateActivityResponse(

        @Schema(description = "모임 ID")
        Long activityId,

        @Schema(description = "동아리 ID (개인 모임이면 null)")
        Long clubId,

        @Schema(description = "동아리 이름 (개인 모임이면 null)")
        String clubName,

        @Schema(description = "공개 타입")
        OpenType openType,

        @Schema(description = "모임 이름")
        String name,

        @Schema(description = "모임 설명")
        String description,

        @Schema(description = "최대 모집 인원")
        Integer capacity,

        @Schema(description = "출석 체크 여부")
        Boolean isAttendanceCheck,

        @Schema(description = "검색 노출 여부")
        Boolean isSearchable,

        @Schema(description = "시작 시간")
        OffsetDateTime startAt,

        @Schema(description = "종료 시간")
        OffsetDateTime endAt,

        @Schema(description = "생성 시간")
        OffsetDateTime createdAt,

        @Schema(description = "카테고리 ID")
        Long categoryId,

        @Schema(description = "카테고리 이름")
        String categoryName,

        @Schema(description = "지역 ID")
        Long regionId,

        @Schema(description = "지역 이름")
        String regionName,

        @Schema(description = "활동 방식")
        ActivityType activityType,

        @Schema(description = "본인인증 필수 여부")
        Boolean isVerificationRequired,

        @Schema(description = "최소 연령")
        Integer minAge,

        @Schema(description = "최대 연령")
        Integer maxAge,

        @Schema(description = "성별 제한")
        Gender gender
) {
}
