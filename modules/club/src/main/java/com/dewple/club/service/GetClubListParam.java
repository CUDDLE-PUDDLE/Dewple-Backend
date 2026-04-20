package com.dewple.club.service;

import com.dewple.common.enums.ActivityType;
import org.springframework.data.domain.Pageable;

public record GetClubListParam(
        Boolean isVerificationRequired,
        Boolean isRecruiting,
        Long categoryId,
        Long regionId,
        ActivityType activityType,
        Pageable pageable
) {
}
