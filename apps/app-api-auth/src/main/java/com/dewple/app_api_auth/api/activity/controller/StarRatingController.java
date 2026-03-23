package com.dewple.app_api_auth.api.activity.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.activity.service.StarRatingService;
import com.dewple.activity.service.StarRatingService.RatingTargetResult;
import com.dewple.app_api_auth.api.activity.dto.RateParticipantRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Star Rating", description = "별점 평가 API")
@RestController
@RequestMapping("/activities/{activityId}/ratings")
@RequiredArgsConstructor
public class StarRatingController {

    private final StarRatingService starRatingService;

    @Operation(summary = "별점 평가", description = "모임 종료 후 7일 이내, 참여 확정자 간 상호 익명 평가. 자기 자신 불가.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> rateParticipant(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody RateParticipantRequest request
    ) {
        starRatingService.rateParticipant(userId, activityId, request.rateeId(), request.score());
        return ApiResponse.ok();
    }

    @Operation(summary = "평가 대상 목록", description = "해당 모임에서 별점 평가할 수 있는 참여자 목록을 조회합니다. 이미 평가 여부, No-show 여부 포함.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/targets")
    public ApiResponse<List<RatingTargetResult>> getRatingTargets(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        List<RatingTargetResult> targets = starRatingService.getRatingTargets(userId, activityId);
        return ApiResponse.ok(targets);
    }

    @Operation(summary = "No-show 지정", description = "모임장/관리자가 참여자를 No-show로 지정합니다. 출석체크 없는 모임에서 수동 지정 용도.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/no-show/{participantId}")
    public ApiResponse<Void> markNoShow(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long participantId
    ) {
        starRatingService.markNoShow(userId, activityId, participantId);
        return ApiResponse.ok();
    }
}
