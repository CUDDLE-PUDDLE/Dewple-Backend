package com.dewple.recruitment.service;

import com.dewple.club.annotation.RequireClubPermission;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.entity.RecruitmentPosting;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.repository.ApplicationRepository;
import com.dewple.recruitment.repository.RecruitmentPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationManageService {

    private final ApplicationRepository applicationRepository;
    private final RecruitmentPostingRepository recruitmentPostingRepository;

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    @Transactional(readOnly = true)
    public Page<ApplicationListResult> getApplicationList(Long clubId, Long userId, Long postingId,
                                                          ApplicationStatus status, String keyword,
                                                          Pageable pageable) {
        findAndValidatePosting(postingId, clubId);

        Page<Application> applications = applicationRepository.searchByPostingId(
                postingId, status, keyword, pageable);

        return applications.map(this::toListResult);
    }

    @RequireClubPermission(Permission.MANAGE_RECRUITMENT)
    @Transactional(readOnly = true)
    public ApplicationDetailResult getApplicationDetail(Long clubId, Long userId, Long postingId, Long applicationId) {
        findAndValidatePosting(postingId, clubId);

        Application application = applicationRepository.findActiveByIdAndPostingId(applicationId, postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));

        return toDetailResult(application);
    }

    @RequireClubPermission(Permission.DECIDE_ADMISSION)
    public void changeApplicationStatus(Long clubId, Long userId, Long postingId, Long applicationId,
                                         ApplicationStatus newStatus) {
        findAndValidatePosting(postingId, clubId);

        Application application = applicationRepository.findActiveByIdAndPostingId(applicationId, postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));

        application.changeApplicationStatus(newStatus);
    }

    @RequireClubPermission(Permission.DECIDE_ADMISSION)
    public void batchChangeApplicationStatus(Long clubId, Long userId, Long postingId,
                                              List<Long> applicationIds, ApplicationStatus newStatus) {
        findAndValidatePosting(postingId, clubId);

        List<Application> applications = applicationRepository.findActiveAllByIdsAndPostingId(applicationIds, postingId);

        if (applications.size() != applicationIds.size()) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_SOME_NOT_FOUND);
        }

        for (Application application : applications) {
            application.changeApplicationStatus(newStatus);
        }
    }

    private RecruitmentPosting findAndValidatePosting(Long postingId, Long clubId) {
        RecruitmentPosting posting = recruitmentPostingRepository.findById(postingId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND));

        if (!posting.getClub().getId().equals(clubId)) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }

        return posting;
    }

    private ApplicationListResult toListResult(Application application) {
        String name;
        String phone;

        if (application.getApplicant() != null) {
            name = application.getApplicant().getName();
            phone = application.getApplicant().getPhone();
        } else {
            name = "비회원";
            phone = application.getGuestPhone();
        }

        return new ApplicationListResult(
                application.getId(),
                name,
                phone,
                application.getCreatedAt(),
                application.getApplicationStatus()
        );
    }

    private ApplicationDetailResult toDetailResult(Application application) {
        String name;
        String phone;

        if (application.getApplicant() != null) {
            name = application.getApplicant().getName();
            phone = application.getApplicant().getPhone();
        } else {
            name = "비회원";
            phone = application.getGuestPhone();
        }

        return new ApplicationDetailResult(
                application.getId(),
                name,
                phone,
                application.getAnswers(),
                application.getCreatedAt(),
                application.getApplicationStatus()
        );
    }
}
