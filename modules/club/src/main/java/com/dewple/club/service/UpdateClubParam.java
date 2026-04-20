package com.dewple.club.service;

import com.dewple.common.enums.ActivityType;

import java.time.LocalDate;
import java.util.List;

public record UpdateClubParam(
        String name,
        String description,
        String coverImg,
        ActivityType activityType,
        LocalDate foundedDate,
        List<Long> categoryIds,
        List<Long> regionIds
) {
}
