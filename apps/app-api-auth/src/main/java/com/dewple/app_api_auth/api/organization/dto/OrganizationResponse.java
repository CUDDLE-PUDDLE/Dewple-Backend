package com.dewple.app_api_auth.api.organization.dto;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.ApprovalStatus;
import com.dewple.common.enums.OrganizationType;
import com.dewple.organization.entity.Organization;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연합회 응답")
public record OrganizationResponse(
        Long id,
        String name,
        String purpose,
        OrganizationType type,
        ActivityType activityType,
        ApprovalStatus approvalStatus,
        Long creatorId
) {
    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.getId(), org.getName(), org.getPurpose(),
                org.getType(), org.getActivityType(),
                org.getApprovalStatus(), org.getCreator().getId()
        );
    }
}
