package com.dewple.app_api_auth.api.organization.dto;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;
import com.dewple.organization.service.OrganizationDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "연합회 단건 조회 응답")
public record GetOrganizationDetailResponse(

        @Schema(description = "연합회 ID")
        Long id,

        @Schema(description = "연합회 이름")
        String name,

        @Schema(description = "연합회 설명")
        String description,

        @Schema(description = "커버 이미지 URL")
        String coverImg,

        @Schema(description = "소개페이지 (JSON)")
        String landingPage,

        @Schema(description = "연합회 종류")
        OrganizationType type,

        @Schema(description = "활동 방식")
        ActivityType activityType,

        @Schema(description = "설립일")
        LocalDate foundedDate,

        @Schema(description = "운영 목적")
        String purpose,

        @Schema(description = "카테고리 ID 목록 (JSON)")
        String categoryIds,

        @Schema(description = "지역 ID 목록 (JSON)")
        String regionIds,

        @Schema(description = "생성자 ID")
        Long creatorId
) {
    public static GetOrganizationDetailResponse from(OrganizationDetailResult result) {
        return new GetOrganizationDetailResponse(
                result.id(),
                result.name(),
                result.description(),
                result.coverImg(),
                result.landingPage(),
                result.type(),
                result.activityType(),
                result.foundedDate(),
                result.purpose(),
                result.categoryIds(),
                result.regionIds(),
                result.creatorId()
        );
    }
}
