package com.dewple.app_api_auth.api.club.dto;

import com.dewple.club.service.ClubRoleResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "동아리 역할 응답")
public record ClubRoleResponse(
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
    public static ClubRoleResponse from(ClubRoleResult result) {
        return new ClubRoleResponse(
                result.id(), result.name(), result.permissions(),
                result.isStaff(), result.isDefault()
        );
    }
}
