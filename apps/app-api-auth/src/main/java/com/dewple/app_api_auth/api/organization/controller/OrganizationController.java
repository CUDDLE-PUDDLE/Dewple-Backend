package com.dewple.app_api_auth.api.organization.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.organization.dto.CreateOrganizationRequest;
import com.dewple.app_api_auth.api.organization.dto.GetOrganizationDetailResponse;
import com.dewple.app_api_auth.api.organization.dto.GetOrganizationListResponse;
import com.dewple.app_api_auth.api.organization.dto.OrganizationResponse;
import com.dewple.app_api_auth.api.organization.dto.RejectOrganizationRequest;
import com.dewple.app_api_auth.api.organization.dto.RequestDissolutionRequest;
import com.dewple.app_api_auth.api.organization.dto.UpdateOrganizationRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.response.SliceResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.service.CreateOrganizationParam;
import com.dewple.organization.service.GetOrganizationListParam;
import com.dewple.organization.service.OrganizationDetailResult;
import com.dewple.organization.service.OrganizationService;
import com.dewple.organization.service.OrganizationSummaryResult;
import com.dewple.organization.service.UpdateOrganizationParam;
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

import java.util.List;

@Tag(name = "Organization", description = "연합회 API")
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "연합회 목록 조회", description = "승인된 연합회 목록을 조회합니다. 카테고리, 지역, 활동방식, 종류로 필터링할 수 있습니다.")
    @GetMapping
    public ApiResponse<SliceResponse<GetOrganizationListResponse>> getOrganizationList(
            @Parameter(description = "카테고리 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "지역 ID") @RequestParam(required = false) Long regionId,
            @Parameter(description = "활동 방식") @RequestParam(required = false) ActivityType activityType,
            @Parameter(description = "연합회 종류") @RequestParam(required = false) OrganizationType type,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        GetOrganizationListParam param = new GetOrganizationListParam(
                categoryId, regionId, activityType, type, pageable
        );
        Slice<OrganizationSummaryResult> results = organizationService.getOrganizationList(param);
        Slice<GetOrganizationListResponse> responseSlice = results.map(GetOrganizationListResponse::from);
        return ApiResponse.ok(SliceResponse.from(responseSlice));
    }

    @Operation(summary = "연합회 생성 신청", description = "연합회 생성을 서비스 관리자에게 신청합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrganizationResponse> apply(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        CreateOrganizationParam param = new CreateOrganizationParam(
                request.name(), request.purpose(), request.type(),
                request.activityType(), request.contactEmail(),
                request.contactPhone(), request.contactPreference(),
                request.targetClubsDescription(), request.targetClubIds(),
                request.categoryIds(), request.regionIds()
        );

        Organization organization = organizationService.apply(userId, param);
        return ApiResponse.ok(OrganizationResponse.from(organization));
    }

    @Operation(summary = "연합회 단건 조회", description = "승인된 연합회의 기본 정보와 소개페이지를 조회합니다.")
    @GetMapping("/{organizationId}")
    public ApiResponse<GetOrganizationDetailResponse> getOrganizationDetail(@PathVariable Long organizationId) {
        OrganizationDetailResult result = organizationService.getOrganizationDetail(organizationId);
        return ApiResponse.ok(GetOrganizationDetailResponse.from(result));
    }

    @Operation(summary = "연합회 정보 수정", description = "연합회 기본 정보를 수정합니다. 승인된 연합회만 수정 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{organizationId}")
    public ApiResponse<Void> updateOrganization(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        UpdateOrganizationParam param = new UpdateOrganizationParam(
                request.name(), request.description(), request.coverImg(),
                request.type(), request.activityType(), request.purpose(),
                request.contactEmail(), request.contactPhone(), request.contactPreference(),
                request.targetClubsDescription(),
                request.categoryIds(), request.regionIds()
        );

        organizationService.updateOrganization(userId, organizationId, param);
        return ApiResponse.ok();
    }

    @Operation(summary = "연합회 해산 신청", description = "연합회 해산을 서비스 관리자에게 신청합니다. 해산 사유는 필수입니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{organizationId}/dissolution")
    public ApiResponse<Void> requestDissolution(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @Valid @RequestBody RequestDissolutionRequest request
    ) {
        organizationService.requestDissolution(userId, organizationId, request.reason());
        return ApiResponse.ok();
    }

    @Operation(summary = "연합회 해산 승인 (관리자)", description = "연합회 해산을 승인합니다. 승인 후 1일 유예 기간이 시작됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{organizationId}/dissolution/approve")
    public ApiResponse<Void> approveDissolution(@PathVariable Long organizationId) {
        organizationService.approveDissolution(organizationId);
        return ApiResponse.ok();
    }

    @Operation(summary = "연합회 해산 취소", description = "유예 기간(1일) 중 대표만 해산을 취소할 수 있습니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{organizationId}/dissolution")
    public ApiResponse<Void> cancelDissolution(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId
    ) {
        organizationService.cancelDissolution(userId, organizationId);
        return ApiResponse.ok();
    }

    @Operation(summary = "연합회 생성 승인 (관리자)")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{organizationId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long organizationId) {
        organizationService.approve(organizationId);
        return ApiResponse.ok();
    }

    @Operation(summary = "연합회 생성 반려 (관리자)")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{organizationId}/reject")
    public ApiResponse<Void> reject(
            @PathVariable Long organizationId,
            @Valid @RequestBody RejectOrganizationRequest request
    ) {
        organizationService.reject(organizationId, request.reason());
        return ApiResponse.ok();
    }

    @Operation(summary = "연합회 생성 신청 목록 (관리자)")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/pending")
    public ApiResponse<List<OrganizationResponse>> getPendingApplications() {
        List<OrganizationResponse> responses = organizationService.getPendingApplications().stream()
                .map(OrganizationResponse::from)
                .toList();
        return ApiResponse.ok(responses);
    }
}
