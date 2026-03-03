package com.dewple.app_api_auth.api.activity.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.activity.dto.CreateActivityRequest;
import com.dewple.app_api_auth.api.activity.dto.CreateActivityResponse;
import com.dewple.app_api_auth.api.activity.dto.GetActivityDetailResponse;
import com.dewple.app_api_auth.api.activity.dto.GetActivityListResponse;
import com.dewple.app_api_auth.api.activity.dto.GetParticipantListResponse;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.response.SliceResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.activity.service.ActivityListSection;
import com.dewple.activity.service.ActivityService;
import com.dewple.activity.service.ActivitySummaryResult;
import com.dewple.activity.service.CreateActivityParam;
import com.dewple.activity.service.CreateActivityResult;
import com.dewple.activity.service.GetActivityDetailResult;
import com.dewple.activity.service.GetActivityListParam;
import com.dewple.activity.service.ParticipantResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Activity", description = "모임 API")
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "모임 목록 조회", description = "섹션별 모임 목록을 조회합니다. PERSONAL(사설모임), LIKED_CLUBS(관심동아리 모임), MY_CLUBS(내 동아리 모임)")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping
    public ApiResponse<SliceResponse<GetActivityListResponse>> getActivityList(
            @CurrentUserId Long userId,
            @RequestParam ActivityListSection section,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        GetActivityListParam param = new GetActivityListParam(section, pageable);
        Slice<ActivitySummaryResult> results = activityService.getActivityList(userId, param);
        Slice<GetActivityListResponse> responseSlice = results.map(GetActivityListResponse::from);
        return ApiResponse.ok(SliceResponse.from(responseSlice));
    }

    @Operation(summary = "모임 생성", description = "새로운 모임을 생성합니다. 동아리 모임인 경우 clubId를 전달하며, 해당 동아리에서 활동 생성/수정 권한이 필요합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateActivityResponse> createActivity(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateActivityRequest request
    ) {
        CreateActivityParam param = new CreateActivityParam(
                request.clubId(),
                request.openType(),
                request.name(),
                request.description(),
                request.capacity(),
                request.isAttendanceCheck(),
                request.isSearchable(),
                request.startAt(),
                request.endAt(),
                request.categoryId(),
                request.regionId(),
                request.activityType(),
                request.isVerificationRequired(),
                request.minAge(),
                request.maxAge(),
                request.gender()
        );

        CreateActivityResult result = activityService.createActivity(userId, param);

        return ApiResponse.ok(new CreateActivityResponse(
                result.activityId(),
                result.clubId(),
                result.clubName(),
                result.openType(),
                result.name(),
                result.description(),
                result.capacity(),
                result.isAttendanceCheck(),
                result.isSearchable(),
                result.startAt(),
                result.endAt(),
                result.createdAt(),
                result.categoryId(),
                result.categoryName(),
                result.regionId(),
                result.regionName(),
                result.activityType(),
                result.isVerificationRequired(),
                result.minAge(),
                result.maxAge(),
                result.gender()
        ));
    }

    @Operation(summary = "모임 삭제", description = "모임을 삭제합니다. 개인 모임은 생성자만, 동아리 모임은 생성자 또는 MANAGE_ACTIVITY 권한 보유자가 삭제할 수 있습니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{activityId}")
    public ApiResponse<Void> deleteActivity(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        activityService.deleteActivity(userId, activityId);
        return ApiResponse.ok();
    }

    @Operation(summary = "모임 상세 조회", description = "모임의 상세 정보를 조회합니다. 모임 기본 정보와 참가자 목록을 반환합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/{activityId}")
    public ApiResponse<GetActivityDetailResponse> getActivityDetail(
            @PathVariable Long activityId
    ) {
        GetActivityDetailResult result = activityService.getActivityDetail(activityId);

        List<GetActivityDetailResponse.ParticipantResponse> participants = result.participants().stream()
                .map(p -> new GetActivityDetailResponse.ParticipantResponse(
                        p.id(), p.profileImg(), p.name()
                ))
                .toList();

        return ApiResponse.ok(new GetActivityDetailResponse(
                result.activityId(),
                result.name(),
                result.description(),
                result.clubId(),
                result.clubName(),
                result.openType(),
                result.capacity(),
                result.startAt(),
                result.endAt(),
                participants
        ));
    }

    @Operation(summary = "모임 지원자 리스트 조회", description = "모임의 지원자 목록을 조회합니다. 개인 모임은 생성자만, 동아리 모임은 생성자 또는 MANAGE_ACTIVITY 권한 보유자가 조회할 수 있습니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/{activityId}/participants")
    public ApiResponse<SliceResponse<GetParticipantListResponse>> getParticipantList(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Slice<ParticipantResult> results = activityService.getParticipantList(userId, activityId, pageable);
        Slice<GetParticipantListResponse> responseSlice = results.map(GetParticipantListResponse::from);
        return ApiResponse.ok(SliceResponse.from(responseSlice));
    }
}
