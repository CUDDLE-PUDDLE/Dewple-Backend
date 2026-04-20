package com.dewple.activity.service;

import java.time.OffsetDateTime;

public record ActivityHistoryResult(
        Long activityId,
        String activityName,
        String thumbnailUrl,
        String createdBy,
        String clubName,
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
}
