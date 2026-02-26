package com.dewple.recruitment.repository;

import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.recruitment.entity.RecruitmentPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruitmentPostingRepository extends JpaRepository<RecruitmentPosting, Long> {

    Optional<RecruitmentPosting> findFirstByClubIdAndRecruitmentStatusNotOrderByCreatedAtDesc(
            Long clubId, RecruitmentStatus status);
}

