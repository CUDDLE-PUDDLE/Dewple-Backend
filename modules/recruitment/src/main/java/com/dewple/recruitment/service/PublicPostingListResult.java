package com.dewple.recruitment.service;

import com.dewple.common.enums.RecruitmentStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record PublicPostingListResult(
        Long postingId,
        Integer generationNo,
        String title,
        OffsetDateTime endAt,
        List<String> departments,
        Long viewCount,
        RecruitmentStatus recruitmentStatus
) {
}
