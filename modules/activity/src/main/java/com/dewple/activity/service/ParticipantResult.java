package com.dewple.activity.service;

import com.dewple.common.enums.ParticipantStatus;

import java.time.OffsetDateTime;

public record ParticipantResult(
        Long participantId,
        Long userId,
        String profileImg,
        String name,
        ParticipantStatus participantStatus,
        OffsetDateTime appliedAt
) {
}
