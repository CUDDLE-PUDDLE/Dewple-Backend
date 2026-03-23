package com.dewple.app_api_auth.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스토리지 사용량 정보")
public record StorageUsageResponse(
        @Schema(description = "사용 중인 용량 (bytes)")
        long usedBytes,
        @Schema(description = "총 용량 (bytes)")
        long totalBytes,
        @Schema(description = "사용률 (%)")
        double usagePercent
) {
}
