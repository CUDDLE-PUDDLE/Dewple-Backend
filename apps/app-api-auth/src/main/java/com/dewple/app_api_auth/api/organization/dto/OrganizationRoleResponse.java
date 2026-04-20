package com.dewple.app_api_auth.api.organization.dto;

import com.dewple.organization.service.OrganizationRoleResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "연합회 역할 응답")
public record OrganizationRoleResponse(
        @Schema(description = "역할 ID")
        Long id,
        @Schema(description = "역할 이름")
        String name,
        @Schema(description = "권한 목록")
        List<String> permissions,
        @Schema(description = "운영진 여부")
        Boolean isStaff,
        @Schema(description = "기본 역할 여부")
        Boolean isDefault
) {
    public static OrganizationRoleResponse from(OrganizationRoleResult result) {
        return new OrganizationRoleResponse(
                result.id(), result.name(), result.permissions(),
                result.isStaff(), result.isDefault()
        );
    }
}
