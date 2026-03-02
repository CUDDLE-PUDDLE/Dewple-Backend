package com.dewple.activity.service;

import com.dewple.common.enums.OpenType;

import java.time.OffsetDateTime;
import java.util.List;

public record GetActivityDetailResult(
        Long activityId,
        String name,
        String description,
        Long clubId,
        String clubName,
        OpenType openType,
        Integer capacity,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        List<ParticipantInfo> participants
) {
    public record ParticipantInfo(Long id, String profileImg, String name) {
    }
}
