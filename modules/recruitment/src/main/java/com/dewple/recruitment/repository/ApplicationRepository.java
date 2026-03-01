package com.dewple.recruitment.repository;

import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.recruitment.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Query("SELECT a FROM Application a " +
            "WHERE a.recruitmentSchema.recruitmentProcess.posting.id = :postingId " +
            "AND a.applicant.id = :applicantId " +
            "AND a.status = :baseStatus " +
            "AND a.applicationStatus IN :statuses")
    Optional<Application> findByPostingIdAndApplicantIdAndStatuses(
            @Param("postingId") Long postingId,
            @Param("applicantId") Long applicantId,
            @Param("baseStatus") BaseStatus baseStatus,
            @Param("statuses") List<ApplicationStatus> statuses);

    @Query("SELECT a FROM Application a " +
            "WHERE a.recruitmentSchema.recruitmentProcess.posting.id = :postingId " +
            "AND a.guestPhone = :guestPhone " +
            "AND a.status = :baseStatus " +
            "AND a.applicationStatus IN :statuses")
    Optional<Application> findByPostingIdAndGuestPhoneAndStatuses(
            @Param("postingId") Long postingId,
            @Param("guestPhone") String guestPhone,
            @Param("baseStatus") BaseStatus baseStatus,
            @Param("statuses") List<ApplicationStatus> statuses);

    @Query("SELECT a FROM Application a " +
            "JOIN FETCH a.recruitmentSchema rs " +
            "JOIN FETCH rs.recruitmentProcess rp " +
            "JOIN FETCH rp.posting p " +
            "JOIN FETCH p.club c " +
            "WHERE a.applicant.id = :applicantId " +
            "AND a.status = :baseStatus " +
            "ORDER BY a.createdAt DESC")
    List<Application> findAllByApplicantIdWithPostingAndClub(
            @Param("applicantId") Long applicantId,
            @Param("baseStatus") BaseStatus baseStatus);
}
