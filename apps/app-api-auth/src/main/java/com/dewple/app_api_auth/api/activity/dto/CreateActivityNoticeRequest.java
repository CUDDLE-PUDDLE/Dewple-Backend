package com.dewple.app_api_auth.api.activity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "모임 공지 생성/수정 요청")
public record CreateActivityNoticeRequest(
        @Schema(description = "공지 제목", example = "4월 모임 장소 안내")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
        String title,

        @Schema(description = "공지 내용")
        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @Schema(description = "이미지 URL 목록")
        List<String> imageUrls
) {
}
