package com.dewple.app_api_auth.api.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 코드 조회 응답")
public record GetInviteCodeResponse(
        @Schema(description = "초대 코드") String inviteCode
) {
}
