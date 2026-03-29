package com.dewple.club.service;

import java.time.LocalDate;

public record PromoteToMemberParam(
        Long generationId,
        LocalDate activityEndDate
) {
}
