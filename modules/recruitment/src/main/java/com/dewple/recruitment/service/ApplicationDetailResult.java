package com.dewple.recruitment.service;

import com.dewple.common.enums.ApplicationStatus;

import java.time.OffsetDateTime;

public record ApplicationDetailResult(
        Long applicationId,
        String applicantName,
        String applicantPhone,
        String answers,
        OffsetDateTime appliedAt,
        ApplicationStatus applicationStatus
) {
}
