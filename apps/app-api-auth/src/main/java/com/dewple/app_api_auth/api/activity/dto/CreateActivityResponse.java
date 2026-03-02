package com.dewple.app_api_auth.api.activity.dto;

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
        OffsetDateTime createdAt
) {
}
