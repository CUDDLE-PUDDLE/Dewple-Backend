package com.dewple.recruitment.service;

import com.dewple.common.enums.ApplicationStatus;

import java.time.OffsetDateTime;

public record ApplicationListResult(
        Long applicationId,
        String applicantName,
        String applicantPhone,
        OffsetDateTime appliedAt,
        ApplicationStatus applicationStatus
) {
}
