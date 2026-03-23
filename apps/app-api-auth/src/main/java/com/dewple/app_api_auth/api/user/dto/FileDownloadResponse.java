package com.dewple.app_api_auth.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 다운로드 URL 응답")
public record FileDownloadResponse(
        @Schema(description = "파일 다운로드 URL (10분간 유효)")
        String downloadUrl
) {
}
