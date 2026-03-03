package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.common.enums.OpenType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "모임 상세 조회 응답")
public record GetActivityDetailResponse(

        @Schema(description = "모임 ID")
        Long activityId,

        @Schema(description = "모임 이름")
        String name,

        @Schema(description = "모임 설명")
        String description,

        @Schema(description = "동아리 ID (개인 모임이면 null)")
        Long clubId,

        @Schema(description = "동아리 이름 (개인 모임이면 null)")
        String clubName,

        @Schema(description = "공개 타입")
        OpenType openType,

        @Schema(description = "최대 모집 인원 (null이면 제한 없음)")
        Integer capacity,

        @Schema(description = "시작 시간")
        OffsetDateTime startAt,

        @Schema(description = "종료 시간")
        OffsetDateTime endAt,

        @Schema(description = "참가자 목록")
        List<ParticipantResponse> participants
) {
    @Schema(description = "참가자 정보")
    public record ParticipantResponse(

            @Schema(description = "참가자 ID")
            Long id,

            @Schema(description = "프로필 이미지 URL")
            String profileImg,

            @Schema(description = "참가자 이름")
            String name
    ) {
    }
}
