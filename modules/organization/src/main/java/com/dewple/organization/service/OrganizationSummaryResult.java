package com.dewple.organization.service;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;

public record OrganizationSummaryResult(
        Long id,
        String name,
        String coverImg,
        OrganizationType type,
        ActivityType activityType,
        String categoryIds,
        String regionIds
) {
}
