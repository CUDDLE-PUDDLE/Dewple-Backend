package com.dewple.organization.service;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.OrganizationType;
import com.dewple.organization.entity.Organization;

import java.time.LocalDate;

public record OrganizationDetailResult(
        Long id,
        String name,
        String description,
        String coverImg,
        String landingPage,
        OrganizationType type,
        ActivityType activityType,
        LocalDate foundedDate,
        String purpose,
        String categoryIds,
        String regionIds,
        Long creatorId
) {
    public static OrganizationDetailResult from(Organization org) {
        return new OrganizationDetailResult(
                org.getId(),
                org.getName(),
                org.getDescription(),
                org.getCoverImg(),
                org.getLandingPage(),
                org.getType(),
                org.getActivityType(),
                org.getFoundedDate(),
                org.getPurpose(),
                org.getCategoryIds(),
                org.getRegionIds(),
                org.getCreator().getId()
        );
    }
}
