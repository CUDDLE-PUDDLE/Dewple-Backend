package com.dewple.club.service;

import com.dewple.common.enums.ActivityType;

import java.time.LocalDate;
import java.util.List;

public record CreateClubResult(
        Long clubId,
        String name,
        Boolean isVerificationRequired,
        ActivityType activityType,
        LocalDate foundedDate,
        List<Long> categoryIds,
        List<Long> regionIds,
        Long creatorId
) {
}
