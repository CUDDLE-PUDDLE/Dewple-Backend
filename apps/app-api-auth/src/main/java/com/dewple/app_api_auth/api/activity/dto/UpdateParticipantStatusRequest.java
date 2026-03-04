package com.dewple.app_api_auth.api.activity.dto;

import com.dewple.common.enums.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모임 지원자 상태 변경 요청")
public record UpdateParticipantStatusRequest(
        @Schema(description = "변경할 상태 (APPROVED: 확정, REJECTED: 불가)", example = "APPROVED")
        @NotNull(message = "지원 상태는 필수입니다.")
        ParticipantStatus participantStatus
) {
}
