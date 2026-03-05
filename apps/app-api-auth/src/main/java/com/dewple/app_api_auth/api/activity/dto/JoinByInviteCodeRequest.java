package com.dewple.app_api_auth.api.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "초대 코드로 모임 참여 요청")
public record JoinByInviteCodeRequest(
        @Schema(description = "초대 코드")
        @NotBlank(message = "초대 코드는 필수입니다.") String inviteCode
) {
}
