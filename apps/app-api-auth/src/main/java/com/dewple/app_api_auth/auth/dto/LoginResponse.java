package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "탈퇴 진행 중 여부 (true면 탈퇴 취소 안내 필요)")
        boolean inDeletionPeriod
) {
}
