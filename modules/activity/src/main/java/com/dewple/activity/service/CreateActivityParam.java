package com.dewple.activity.service;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.OpenType;

import java.time.OffsetDateTime;

public record CreateActivityParam(
        Long clubId,
        OpenType openType,
        String name,
        String description,
        Integer capacity,
        Boolean isAttendanceCheck,
        Boolean isSearchable,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String emergencyContact,
        Integer cancelDeadlineDays,
        Long categoryId,
        Long regionId,
        ActivityType activityType,
        Boolean isVerificationRequired,
        Integer minAge,
        Integer maxAge,
        Gender gender
) {
}
