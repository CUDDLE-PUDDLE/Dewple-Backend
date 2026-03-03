package com.dewple.recruitment.repository;

import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.recruitment.entity.RecruitmentPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruitmentPostingRepository extends JpaRepository<RecruitmentPosting, Long> {

    Optional<RecruitmentPosting> findFirstByClubIdAndRecruitmentStatusNotOrderByCreatedAtDesc(
            Long clubId, RecruitmentStatus status);

    @Modifying
    @Query("UPDATE RecruitmentPosting p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postingId")
    int incrementViewCount(@Param("postingId") Long postingId);

    @Query("SELECT DISTINCT p FROM RecruitmentPosting p " +
            "JOIN FETCH p.generation g " +
            "LEFT JOIN FETCH p.recruitmentDepartments rd " +
            "LEFT JOIN FETCH rd.department " +
            "WHERE p.club.id = :clubId " +
            "AND p.recruitmentStatus IN :statuses " +
            "AND p.status = :baseStatus")
    List<RecruitmentPosting> findPostingsForPublicList(
            @Param("clubId") Long clubId,
            @Param("statuses") List<RecruitmentStatus> statuses,
            @Param("baseStatus") BaseStatus baseStatus);

    @Query("SELECT p FROM RecruitmentPosting p " +
            "JOIN FETCH p.club c " +
            "JOIN FETCH p.generation g " +
            "LEFT JOIN FETCH p.recruitmentDepartments rd " +
            "LEFT JOIN FETCH rd.department " +
            "WHERE p.id = :postingId " +
            "AND p.club.id = :clubId " +
            "AND p.recruitmentStatus <> 'DRAFT' " +
            "AND p.status = :baseStatus")
    Optional<RecruitmentPosting> findPostingDetailForPublic(
            @Param("postingId") Long postingId,
            @Param("clubId") Long clubId,
            @Param("baseStatus") BaseStatus baseStatus);
}

