package com.dewple.app_api_auth.api.activity.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.activity.service.ActivityReportService;
import com.dewple.app_api_auth.api.activity.dto.CreateReportRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Activity Report", description = "모임 신고 API")
@RestController
@RequestMapping("/activities/{activityId}/reports")
@RequiredArgsConstructor
public class ActivityReportController {

    private final ActivityReportService reportService;

    @Operation(summary = "모임 신고", description = "모임을 신고합니다. 동일 대상에 7일 이내 중복 신고 불가합니다. 모임 정보 스냅샷이 자동 저장됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> reportActivity(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        reportService.reportActivity(userId, activityId, request.category(), request.reason());
        return ApiResponse.ok();
    }
}
