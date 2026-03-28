package com.dewple.club.service;

import com.dewple.common.enums.ActivityType;

public record ClubSummaryResult(
        Long id,
        String name,
        String coverImg,
        ActivityType activityType,
        Boolean isVerificationRequired,
        Integer likeCount,
        Long memberCount
) {
}
