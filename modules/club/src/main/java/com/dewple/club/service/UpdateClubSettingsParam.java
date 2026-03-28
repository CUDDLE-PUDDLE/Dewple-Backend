package com.dewple.club.service;

import com.dewple.common.enums.Gender;

public record UpdateClubSettingsParam(
        Boolean isVerificationRequired,
        Gender gender,
        Long minAge,
        Long maxAge
) {
}
