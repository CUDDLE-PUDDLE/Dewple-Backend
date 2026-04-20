package com.dewple.app_api_auth.api.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 작성 요청")
public record CreateCommentRequest(
        @Schema(description = "댓글 내용")
        @NotBlank(message = "내용은 필수입니다.")
        String content
) {
}
