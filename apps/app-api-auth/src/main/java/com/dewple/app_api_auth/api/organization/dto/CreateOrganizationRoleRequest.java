package com.dewple.app_api_auth.api.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "연합회 역할 생성 요청")
public record CreateOrganizationRoleRequest(
        @Schema(description = "역할 이름", example = "홍보담당")
        @NotBlank(message = "역할 이름은 필수입니다.")
        @Size(max = 50, message = "역할 이름은 50자 이내여야 합니다.")
        String name,

        @Schema(description = "권한 목록", example = "[\"MANAGE_NOTICE\", \"MANAGE_FEED\"]")
        @NotNull(message = "권한 목록은 필수입니다.")
        List<String> permissions,

        @Schema(description = "운영진 여부", example = "true")
        @NotNull(message = "운영진 여부는 필수입니다.")
        Boolean isStaff
) {
}
