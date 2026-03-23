package com.dewple.app_api_auth.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "아이디 변경 요청")
public record ChangeUserIdRequest(
        @Schema(description = "새 아이디", example = "newdewple123")
        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]{3,32}$", message = "아이디는 영문, 숫자 3~32자여야 합니다.")
        String newUserId
) {
}
