package com.dewple.app_api_auth.api.organization.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.organization.dto.CreateOrganizationRequest;
import com.dewple.app_api_auth.api.organization.dto.OrganizationResponse;
import com.dewple.app_api_auth.api.organization.dto.RejectOrganizationRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.service.CreateOrganizationParam;
import com.dewple.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Organization", description = "연합회 API")
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

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
                request.targetClubsDescription(),
                request.categoryIds(), request.regionIds()
        );

        Organization organization = organizationService.apply(userId, param);
        return ApiResponse.ok(OrganizationResponse.from(organization));
    }

    @Operation(summary = "연합회 단건 조회")
    @GetMapping("/{organizationId}")
    public ApiResponse<OrganizationResponse> getOrganization(@PathVariable Long organizationId) {
        Organization organization = organizationService.getById(organizationId);
        return ApiResponse.ok(OrganizationResponse.from(organization));
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
