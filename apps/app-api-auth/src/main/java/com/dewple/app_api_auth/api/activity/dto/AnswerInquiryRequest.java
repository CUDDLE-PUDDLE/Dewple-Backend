package com.dewple.app_api_auth.api.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "문의 답변/수정 요청")
public record AnswerInquiryRequest(
        @Schema(description = "답변 내용")
        @NotBlank(message = "답변 내용은 필수입니다.")
        String answer
) {
}
