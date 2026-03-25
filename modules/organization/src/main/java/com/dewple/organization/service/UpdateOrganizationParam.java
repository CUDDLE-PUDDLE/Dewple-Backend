package com.dewple.organization.service;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.ContactPreference;
import com.dewple.common.enums.OrganizationType;

import java.util.List;

public record UpdateOrganizationParam(
        String name,
        String description,
        String coverImg,
        OrganizationType type,
        ActivityType activityType,
        String purpose,
        String contactEmail,
        String contactPhone,
        ContactPreference contactPreference,
        String targetClubsDescription,
        List<Long> categoryIds,
        List<Long> regionIds
) {
}
