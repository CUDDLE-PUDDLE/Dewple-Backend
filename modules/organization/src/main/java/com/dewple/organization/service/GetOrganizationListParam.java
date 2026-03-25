package com.dewple.organization.service;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;
import org.springframework.data.domain.Pageable;

public record GetOrganizationListParam(
        Long categoryId,
        Long regionId,
        ActivityType activityType,
        OrganizationType type,
        Pageable pageable
) {
}
