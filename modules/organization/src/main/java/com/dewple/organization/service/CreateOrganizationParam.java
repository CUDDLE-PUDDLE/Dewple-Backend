package com.dewple.organization.service;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.ContactPreference;
import com.dewple.common.enums.OrganizationType;

import java.util.List;

public record CreateOrganizationParam(
        String name,
        String purpose,
        OrganizationType type,
        ActivityType activityType,
        String contactEmail,
        String contactPhone,
        ContactPreference contactPreference,
        String targetClubsDescription,
        List<Long> targetClubIds,
        List<Long> categoryIds,
        List<Long> regionIds
) {
}
