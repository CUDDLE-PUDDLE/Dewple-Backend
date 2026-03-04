package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.activity.service.ParticipantResult;
import com.dewple.common.enums.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "지원자 목록 응답")
public record GetParticipantListResponse(
        @Schema(description = "지원자 ID", example = "1")
        Long participantId,

        @Schema(description = "사용자 ID", example = "2")
        Long userId,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/img.jpg")
        String profileImg,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "지원 상태", example = "PENDING")
        ParticipantStatus participantStatus,

        @Schema(description = "지원 일시", example = "2026-03-01T10:00:00+09:00")
        OffsetDateTime appliedAt
) {
    public static GetParticipantListResponse from(ParticipantResult result) {
        return new GetParticipantListResponse(
                result.participantId(),
                result.userId(),
                result.profileImg(),
                result.name(),
                result.participantStatus(),
                result.appliedAt()
        );
    }
}
