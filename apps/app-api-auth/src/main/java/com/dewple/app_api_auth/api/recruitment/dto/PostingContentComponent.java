package com.dewple.app_api_auth.api.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostingContentComponent(

        @NotNull(message = "정렬 순서는 필수입니다.")
        Integer orderNumber,

        String title,

        String imageUrl,

        @NotBlank(message = "텍스트 내용은 필수입니다.")
        String text
) {
}
