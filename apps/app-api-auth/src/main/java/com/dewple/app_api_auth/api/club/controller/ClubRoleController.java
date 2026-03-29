package com.dewple.app_api_auth.api.club.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.club.dto.ClubRoleResponse;
import com.dewple.app_api_auth.api.club.dto.CreateClubRoleRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.club.service.ClubRoleResult;
import com.dewple.club.service.ClubRoleService;
import com.dewple.app_api_auth.api.club.dto.UpdateClubRoleRequest;
import com.dewple.club.service.CreateClubRoleParam;
import com.dewple.club.service.UpdateClubRoleParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Club Role", description = "동아리 역할 관리 API")
@RestController
@RequestMapping("/clubs/{clubId}/roles")
@RequiredArgsConstructor
public class ClubRoleController {

    private final ClubRoleService clubRoleService;

    @Operation(summary = "역할 생성", description = "커스텀 역할을 생성합니다. 이름 + 권한 조합 + 운영진 여부를 설정합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClubRoleResponse> createRole(
            @CurrentUserId Long userId,
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubRoleRequest request
    ) {
        CreateClubRoleParam param = new CreateClubRoleParam(
                request.name(), request.permissions(), request.isStaff()
        );
        ClubRoleResult result = clubRoleService.createRole(userId, clubId, param);
        return ApiResponse.ok(ClubRoleResponse.from(result));
    }

    @Operation(summary = "역할 수정", description = "역할의 이름, 권한, 운영진 여부를 수정합니다. 회장 역할은 수정할 수 없습니다. 기본 역할은 회장만 수정 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{roleId}")
    public ApiResponse<ClubRoleResponse> updateRole(
            @CurrentUserId Long userId,
            @PathVariable Long clubId,
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateClubRoleRequest request
    ) {
        UpdateClubRoleParam param = new UpdateClubRoleParam(
                request.name(), request.permissions(), request.isStaff()
        );
        ClubRoleResult result = clubRoleService.updateRole(userId, clubId, roleId, param);
        return ApiResponse.ok(ClubRoleResponse.from(result));
    }

    @Operation(summary = "역할 목록 조회", description = "동아리의 기본 역할 및 커스텀 역할 목록을 조회합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping
    public ApiResponse<List<ClubRoleResponse>> getRoles(@PathVariable Long clubId) {
        List<ClubRoleResponse> responses = clubRoleService.getRoles(clubId).stream()
                .map(ClubRoleResponse::from)
                .toList();
        return ApiResponse.ok(responses);
    }
}
