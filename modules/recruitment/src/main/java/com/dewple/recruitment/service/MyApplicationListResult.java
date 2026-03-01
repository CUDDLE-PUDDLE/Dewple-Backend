package com.dewple.recruitment.service;

import com.dewple.common.enums.ApplicationStatus;

import java.time.OffsetDateTime;

public record MyApplicationListResult(
        Long applicationId,
        Long postingId,
        Long clubId,
        String clubName,
        String postingTitle,
        ApplicationStatus applicationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
