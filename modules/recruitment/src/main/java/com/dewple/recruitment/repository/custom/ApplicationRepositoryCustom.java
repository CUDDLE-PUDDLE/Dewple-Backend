package com.dewple.recruitment.repository.custom;

import com.dewple.common.enums.ApplicationStatus;
import com.dewple.recruitment.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepositoryCustom {

    Page<Application> searchByPostingId(Long postingId, ApplicationStatus status, String keyword, Pageable pageable);

    Optional<Application> findByPostingIdAndApplicantIdAndStatuses(
            Long postingId, Long applicantId, List<ApplicationStatus> statuses);

    Optional<Application> findByPostingIdAndGuestPhoneAndStatuses(
            Long postingId, String guestPhone, List<ApplicationStatus> statuses);

    List<Application> findAllByApplicantIdWithPostingAndClub(Long applicantId);

    Optional<Application> findActiveByIdAndPostingId(Long applicationId, Long postingId);

    List<Application> findActiveAllByIdsAndPostingId(List<Long> applicationIds, Long postingId);
}
