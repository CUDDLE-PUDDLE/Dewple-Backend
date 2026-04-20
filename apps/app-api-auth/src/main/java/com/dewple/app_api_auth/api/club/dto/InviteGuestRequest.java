package com.dewple.app_api_auth.api.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "GUEST 초대 요청")
public record InviteGuestRequest(
        @Schema(description = "초대할 유저 ID", example = "5")
        @NotNull(message = "유저 ID는 필수입니다.")
        Long userId,

        @Schema(description = "연결할 모임 ID 목록", example = "[1, 2]")
        @NotNull(message = "모임 ID는 필수입니다.")
        @Size(min = 1, message = "모임은 최소 1개 지정해야 합니다.")
        List<Long> activityIds
) {
}
