package com.dewple.club.service;

import com.dewple.common.entity.Club;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClubDetailResult(
        Long id,
        String name,
        String description,
        String coverImg,
        String landingPage,
        ActivityType activityType,
        Boolean isVerificationRequired,
        Gender gender,
        Long minAge,
        Long maxAge,
        LocalDate foundedDate,
        Integer likeCount,
        BigDecimal reputationScore,
        Long creatorId
) {
    public static ClubDetailResult from(Club club) {
        return new ClubDetailResult(
                club.getId(), club.getName(), club.getDescription(),
                club.getCoverImg(), club.getLandingPage(),
                club.getActivityType(), club.getIsVerificationRequired(),
                club.getGender(), club.getMinAge(), club.getMaxAge(),
                club.getFoundedDate(), club.getLikeCount(),
                club.getReputationScore(), club.getCreator().getId()
        );
    }
}
