package com.dewple.recruitment.service;

import com.dewple.common.enums.RecruitmentStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PublicPostingDetailResult(
        Long postingId,
        Long clubId,
        String clubName,
        Integer generationNo,
        String title,
        String content,
        String themeColor,
        Integer capacity,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        LocalDate resultDate,
        LocalDate endOfGenerationDate,
        Boolean hasSecondInterview,
        String emergencyContact,
        LocalDate firstAnnouncementDate,
        List<DepartmentInfo> departments,
        List<ProcessInfo> processes,
        String applicationForm,
        Long viewCount,
        RecruitmentStatus recruitmentStatus
) {

    public record DepartmentInfo(
            String name,
            Integer count
    ) {
    }

    public record ProcessInfo(
            Integer processOrder,
            String name,
            String processType,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
    }
}
