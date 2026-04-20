package com.dewple.app_api_auth.api.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "모임 문의 작성 요청")
public record CreateInquiryRequest(
        @Schema(description = "문의 내용")
        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @Schema(description = "익명 여부", example = "false")
        Boolean isAnonymous
) {
}
