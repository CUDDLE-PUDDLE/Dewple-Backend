package com.dewple.activity.service;

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
        OffsetDateTime endAt
) {
}
