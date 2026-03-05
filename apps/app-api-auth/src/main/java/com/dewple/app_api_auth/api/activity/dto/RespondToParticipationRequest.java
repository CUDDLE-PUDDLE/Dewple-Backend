package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.common.enums.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모임 참여 응답 요청")
public record RespondToParticipationRequest(
        @Schema(description = "참여 응답 (CONFIRMED: 참여, DECLINED: 불참)", example = "CONFIRMED")
        @NotNull(message = "참여 응답은 필수입니다.")
        ParticipantStatus participantStatus
) {
}
