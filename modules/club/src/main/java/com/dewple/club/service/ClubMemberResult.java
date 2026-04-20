package com.dewple.club.service;

import com.dewple.club.entity.ClubMember;
import com.dewple.common.enums.ActivityStatus;

import java.math.BigDecimal;

public record ClubMemberResult(
        Long memberId,
        Long userId,
        String name,
        String nickname,
        String profileImg,
        String roleName,
        ActivityStatus activityStatus,
        BigDecimal reputationScore
) {
    public static ClubMemberResult from(ClubMember member) {
        return new ClubMemberResult(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getNickname(),
                member.getUser().getProfileImg(),
                member.getRole().getName(),
                member.getActivityStatus(),
                member.getUser().getReputationScore()
        );
    }
}
