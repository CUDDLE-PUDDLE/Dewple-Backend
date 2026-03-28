package com.dewple.app_api_auth.api.club.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.club.dto.CreateClubRequest;
import com.dewple.app_api_auth.api.club.dto.CreateClubResponse;
import com.dewple.app_api_auth.api.club.dto.GetClubDetailResponse;
import com.dewple.app_api_auth.api.club.dto.GetClubListResponse;
import com.dewple.app_api_auth.api.club.dto.UpdateClubRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.response.SliceResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.club.service.ClubDetailResult;
import com.dewple.club.service.ClubService;
import com.dewple.club.service.ClubSummaryResult;
import com.dewple.club.service.CreateClubParam;
import com.dewple.club.service.CreateClubResult;
import com.dewple.club.service.GetClubListParam;
import com.dewple.club.service.UpdateClubParam;
import com.dewple.common.enums.ActivityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Club", description = "동아리 API")
@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @Operation(summary = "동아리 목록 조회", description = "동아리 목록을 조회합니다. 본인인증/카테고리/지역/활동방식으로 필터링하며, 부원+좋아요 합산순으로 정렬합니다.")
    @GetMapping
    public ApiResponse<SliceResponse<GetClubListResponse>> getClubList(
            @Parameter(description = "본인인증 필수 여부") @RequestParam(required = false) Boolean isVerificationRequired,
            @Parameter(description = "모집중 여부") @RequestParam(required = false) Boolean isRecruiting,
            @Parameter(description = "카테고리 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "지역 ID") @RequestParam(required = false) Long regionId,
            @Parameter(description = "활동 방식") @RequestParam(required = false) ActivityType activityType,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        GetClubListParam param = new GetClubListParam(
                isVerificationRequired, isRecruiting, categoryId, regionId, activityType, pageable
        );
        Slice<ClubSummaryResult> results = clubService.getClubList(param);
        Slice<GetClubListResponse> responseSlice = results.map(GetClubListResponse::from);
        return ApiResponse.ok(SliceResponse.from(responseSlice));
    }

    @Operation(summary = "동아리 단건 조회", description = "동아리의 기본 정보와 소개페이지를 조회합니다.")
    @GetMapping("/{clubId}")
    public ApiResponse<GetClubDetailResponse> getClubDetail(@PathVariable Long clubId) {
        ClubDetailResult result = clubService.getClubDetail(clubId);
        return ApiResponse.ok(GetClubDetailResponse.from(result));
    }

    @Operation(summary = "동아리 생성", description = "동아리를 생성합니다. 생성자가 자동으로 회장이 됩니다. 회장 동시 운영 최대 5개.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateClubResponse> createClub(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateClubRequest request
    ) {
        CreateClubParam param = new CreateClubParam(
                request.name(), request.isVerificationRequired(),
                request.activityType(), request.foundedDate(),
                request.categoryIds(), request.regionIds()
        );

        CreateClubResult result = clubService.createClub(userId, param);
        return ApiResponse.ok(CreateClubResponse.from(result));
    }

    @Operation(summary = "동아리 정보 수정", description = "동아리 기본 정보를 수정합니다. 동아리관리(3번) 권한이 필요합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{clubId}")
    public ApiResponse<Void> updateClub(
            @CurrentUserId Long userId,
            @PathVariable Long clubId,
            @Valid @RequestBody UpdateClubRequest request
    ) {
        UpdateClubParam param = new UpdateClubParam(
                request.name(), request.description(), request.coverImg(),
                request.activityType(), request.foundedDate(),
                request.categoryIds(), request.regionIds()
        );

        clubService.updateClub(userId, clubId, param);
        return ApiResponse.ok();
    }
}
