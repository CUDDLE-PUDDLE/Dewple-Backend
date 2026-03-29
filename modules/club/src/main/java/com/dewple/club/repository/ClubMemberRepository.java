package com.dewple.club.repository;

import com.dewple.club.entity.ClubMember;
import com.dewple.club.entity.ClubRole;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.BaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    Optional<ClubMember> findByClubIdAndUserId(Long clubId, Long userId);
    Optional<ClubMember> findByClubIdAndUserIdAndActivityStatus(Long clubId, Long userId, ActivityStatus activityStatus);

    List<ClubMember> findByUserIdAndStatusAndActivityStatus(Long userId, BaseStatus status, ActivityStatus activityStatus);

    List<ClubMember> findByClubIdAndStatusAndActivityStatusAndUserIdNot(
            Long clubId, BaseStatus status, ActivityStatus activityStatus, Long userId);

    List<ClubMember> findByClubIdAndStatusAndActivityStatus(
            Long clubId, BaseStatus status, ActivityStatus activityStatus);

    List<ClubMember> findByRole(ClubRole role);

    Optional<ClubMember> findByClubIdAndId(Long clubId, Long memberId);

    List<ClubMember> findByClubIdAndActivityStatus(Long clubId, ActivityStatus activityStatus);

    List<ClubMember> findByClubId(Long clubId);
}
