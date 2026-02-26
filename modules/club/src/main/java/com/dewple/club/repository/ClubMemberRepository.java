package com.dewple.club.repository;

import com.dewple.club.entity.ClubMember;
import com.dewple.common.enums.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    Optional<ClubMember> findByClubIdAndUserIdAndActivityStatus(Long clubId, Long userId, ActivityStatus activityStatus);
}
