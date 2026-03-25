package com.dewple.app_api_auth.api.organization.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.organization.dto.CreateOrganizationRoleRequest;
import com.dewple.app_api_auth.api.organization.dto.OrganizationRoleResponse;
import com.dewple.app_api_auth.api.organization.dto.UpdateOrganizationRoleRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.organization.service.CreateOrganizationRoleParam;
import com.dewple.organization.service.OrganizationRoleResult;
import com.dewple.organization.service.OrganizationRoleService;
import com.dewple.organization.service.UpdateOrganizationRoleParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Organization Role", description = "연합회 역할 관리 API")
@RestController
@RequestMapping("/organizations/{organizationId}/roles")
@RequiredArgsConstructor
public class OrganizationRoleController {

    private final OrganizationRoleService organizationRoleService;

    @Operation(summary = "역할 목록 조회", description = "연합회의 기본 역할 및 커스텀 역할 목록을 조회합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping
    public ApiResponse<List<OrganizationRoleResponse>> getRoles(@PathVariable Long organizationId) {
        List<OrganizationRoleResponse> responses = organizationRoleService.getRoles(organizationId).stream()
                .map(OrganizationRoleResponse::from)
                .toList();
        return ApiResponse.ok(responses);
    }

    @Operation(summary = "역할 생성", description = "커스텀 역할을 생성합니다. 이름 + 권한 조합 + 운영진 여부를 설정합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrganizationRoleResponse> createRole(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateOrganizationRoleRequest request
    ) {
        CreateOrganizationRoleParam param = new CreateOrganizationRoleParam(
                request.name(), request.permissions(), request.isStaff()
        );
        OrganizationRoleResult result = organizationRoleService.createRole(userId, organizationId, param);
        return ApiResponse.ok(OrganizationRoleResponse.from(result));
    }

    @Operation(summary = "역할 수정", description = "커스텀 역할의 이름, 권한, 운영진 여부를 수정합니다. 기본 역할은 수정할 수 없습니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{roleId}")
    public ApiResponse<OrganizationRoleResponse> updateRole(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateOrganizationRoleRequest request
    ) {
        UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam(
                request.name(), request.permissions(), request.isStaff()
        );
        OrganizationRoleResult result = organizationRoleService.updateRole(userId, organizationId, roleId, param);
        return ApiResponse.ok(OrganizationRoleResponse.from(result));
    }

    @Operation(summary = "역할 삭제", description = "커스텀 역할을 삭제합니다. 기본 역할은 삭제할 수 없습니다. 부여된 회원은 연합회원으로 자동 전환됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{roleId}")
    public ApiResponse<Void> deleteRole(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @PathVariable Long roleId
    ) {
        organizationRoleService.deleteRole(userId, organizationId, roleId);
        return ApiResponse.ok();
    }
}
