package com.dewple.recruitment.repository;

import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.recruitment.entity.RecruitmentPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruitmentPostingRepository extends JpaRepository<RecruitmentPosting, Long> {

    Optional<RecruitmentPosting> findFirstByClubIdAndRecruitmentStatusNotOrderByCreatedAtDesc(
            Long clubId, RecruitmentStatus status);

    @Modifying
    @Query("UPDATE RecruitmentPosting p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postingId")
    int incrementViewCount(@Param("postingId") Long postingId);
}

