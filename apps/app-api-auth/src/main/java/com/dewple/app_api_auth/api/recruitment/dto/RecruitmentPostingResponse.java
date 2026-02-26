package com.dewple.app_api_auth.api.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모집 공고 응답")
public record RecruitmentPostingResponse(

        @Schema(description = "공고 ID", example = "1")
        Long postingId,

        @Schema(description = "공고 버전", example = "1")
        Long version
) {
}
