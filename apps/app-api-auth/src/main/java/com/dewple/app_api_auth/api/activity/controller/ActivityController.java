package com.dewple.app_api_auth.api.activity.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.activity.dto.CreateActivityRequest;
import com.dewple.app_api_auth.api.activity.dto.CreateActivityResponse;
import com.dewple.app_api_auth.api.activity.dto.GetActivityDetailResponse;
import com.dewple.app_api_auth.api.activity.dto.GetActivityListResponse;
import com.dewple.app_api_auth.api.activity.dto.GetInviteCodeResponse;
import com.dewple.app_api_auth.api.activity.dto.GetParticipantListResponse;
import com.dewple.app_api_auth.api.activity.dto.JoinByInviteCodeRequest;
import com.dewple.app_api_auth.api.activity.dto.RespondToParticipationRequest;
import com.dewple.app_api_auth.api.activity.dto.UpdateParticipantStatusRequest;
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
                request.emergencyContact(),
                request.cancelDeadlineDays(),
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

    @Operation(summary = "모임 수동 취소", description = "모임장만 모임을 취소할 수 있습니다. 모든 참여자에게 취소 알림이 발송됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{activityId}/cancel")
    public ApiResponse<Void> cancelActivity(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        activityService.cancelActivityManually(userId, activityId);
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

    @Operation(summary = "모임 지원자 상태 변경", description = "지원자의 상태를 확정(APPROVED) 또는 불가(REJECTED)로 변경합니다. 개인 모임은 생성자만, 동아리 모임은 생성자 또는 MANAGE_ACTIVITY 권한 보유자가 변경할 수 있습니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{activityId}/participants/{participantId}/status")
    public ApiResponse<Void> updateParticipantStatus(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long participantId,
            @Valid @RequestBody UpdateParticipantStatusRequest request
    ) {
        activityService.updateParticipantStatus(userId, activityId, participantId, request.participantStatus());
        return ApiResponse.ok();
    }

    @Operation(summary = "모임 참여 응답", description = "운영진이 참여 확정(APPROVED)한 지원자가 참여(CONFIRMED) 또는 불참(DECLINED)을 선택합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{activityId}/participation")
    public ApiResponse<Void> respondToParticipation(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody RespondToParticipationRequest request
    ) {
        activityService.respondToParticipation(userId, activityId, request.participantStatus());
        return ApiResponse.ok();
    }

    @Operation(summary = "참여 취소", description = "참여 확정된 모임의 참여를 취소합니다. 모임 시작 전 + 취소 기한 내에만 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{activityId}/participation")
    public ApiResponse<Void> cancelParticipation(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        activityService.cancelParticipation(userId, activityId);
        return ApiResponse.ok();
    }

    @Operation(summary = "관심 모임 추가", description = "모임을 관심 모임으로 등록합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{activityId}/interest")
    public ApiResponse<Void> addActivityInterest(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        activityService.addActivityInterest(userId, activityId);
        return ApiResponse.ok();
    }

    @Operation(summary = "관심 모임 제거", description = "모임을 관심 모임에서 제거합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{activityId}/interest")
    public ApiResponse<Void> removeActivityInterest(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        activityService.removeActivityInterest(userId, activityId);
        return ApiResponse.ok();
    }

    @Operation(summary = "관심 모임 목록 조회", description = "관심 모임으로 등록한 모임 목록을 조회합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/interests")
    public ApiResponse<SliceResponse<GetActivityListResponse>> getInterestedActivities(
            @CurrentUserId Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Slice<ActivitySummaryResult> results = activityService.getInterestedActivities(userId, pageable);
        Slice<GetActivityListResponse> responseSlice = results.map(GetActivityListResponse::from);
        return ApiResponse.ok(SliceResponse.from(responseSlice));
    }

    @Operation(summary = "초대 코드 조회", description = "비공개 개인 모임의 초대 코드를 조회합니다. 모임 생성자만 조회할 수 있습니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/{activityId}/invite-code")
    public ApiResponse<GetInviteCodeResponse> getInviteCode(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        String inviteCode = activityService.getInviteCode(userId, activityId);
        return ApiResponse.ok(new GetInviteCodeResponse(inviteCode));
    }

    @Operation(summary = "초대 코드로 모임 참여", description = "초대 코드를 사용하여 비공개 모임에 참여 신청합니다. PENDING 상태로 등록됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/join")
    public ApiResponse<Void> joinByInviteCode(
            @CurrentUserId Long userId,
            @Valid @RequestBody JoinByInviteCodeRequest request
    ) {
        activityService.joinByInviteCode(userId, request.inviteCode());
        return ApiResponse.ok();
    }

    // ========== 모임관리자 API ==========

    @Operation(summary = "모임관리자 초대 코드 조회", description = "모임장만 조회 가능합니다. 이 코드를 공유하여 모임관리자를 초대합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/{activityId}/manager-invite-code")
    public ApiResponse<GetInviteCodeResponse> getManagerInviteCode(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        String code = activityService.getManagerInviteCode(userId, activityId);
        return ApiResponse.ok(new GetInviteCodeResponse(code));
    }

    @Operation(summary = "모임관리자로 참여", description = "모임관리자 초대 코드를 사용하여 모임관리자로 등록됩니다. 자동 참여 확정되며 최대 모집 인원에 포함되지 않습니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/join-as-manager")
    public ApiResponse<Void> joinAsManager(
            @CurrentUserId Long userId,
            @Valid @RequestBody JoinByInviteCodeRequest request
    ) {
        activityService.joinAsManager(userId, request.inviteCode());
        return ApiResponse.ok();
    }

    @Operation(summary = "모임관리자 제거", description = "모임장이 특정 모임관리자를 제거합니다. 참여 확정도 함께 취소됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{activityId}/managers/{managerId}")
    public ApiResponse<Void> removeManager(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long managerId
    ) {
        activityService.removeManager(userId, activityId, managerId);
        return ApiResponse.ok();
    }

    @Operation(summary = "모임관리자 자발적 탈퇴", description = "본인의 모임관리자 자격을 해제합니다. 참여 확정도 함께 취소됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{activityId}/managers/me")
    public ApiResponse<Void> leaveAsManager(
            @CurrentUserId Long userId,
            @PathVariable Long activityId
    ) {
        activityService.leaveAsManager(userId, activityId);
        return ApiResponse.ok();
    }
}
