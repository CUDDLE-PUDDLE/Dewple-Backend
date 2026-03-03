package com.dewple.activity.service;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.OpenType;

import java.time.OffsetDateTime;

public record CreateActivityResult(
        Long activityId,
        Long clubId,
        String clubName,
        OpenType openType,
        String name,
        String description,
        Integer capacity,
        Boolean isAttendanceCheck,
        Boolean isSearchable,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime createdAt,
        Long categoryId,
        String categoryName,
        Long regionId,
        String regionName,
        ActivityType activityType,
        Boolean isVerificationRequired,
        Integer minAge,
        Integer maxAge,
        Gender gender
) {
}
