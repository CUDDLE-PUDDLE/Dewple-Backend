package com.dewple.recruitment.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ProcessType;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.entity.RecruitmentPosting;
import com.dewple.recruitment.entity.RecruitmentSchema;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.repository.ApplicationRepository;
import com.dewple.recruitment.repository.RecruitmentPostingRepository;
import com.dewple.recruitment.repository.RecruitmentSchemaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewple.common.enums.EditWindowBasis;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final RecruitmentPostingRepository recruitmentPostingRepository;
    private final RecruitmentSchemaRepository recruitmentSchemaRepository;
    private final EntityManager entityManager;

    public Application submitApplication(Long clubId, Long postingId, Long applicantId, SubmitApplicationCommand command) {
        RecruitmentPosting posting = findAndValidatePosting(postingId, clubId);
        validatePostingIsAccepting(posting);

        RecruitmentSchema schema = recruitmentSchemaRepository
                .findLatestByPostingIdAndProcessType(postingId, ProcessType.DOCUMENT)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_SCHEMA_NOT_FOUND));

        return applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                postingId, applicantId,
                List.of(ApplicationStatus.SUBMITTED, ApplicationStatus.TEMPORARY)
        ).map(existing -> {
            if (existing.getApplicationStatus() == ApplicationStatus.SUBMITTED) {
                throw new BusinessException(RecruitmentErrorCode.APPLICATION_ALREADY_SUBMITTED);
            }
            // TEMPORARY → SUBMITTED 전환
            existing.updateAnswers(command.answersJson());
            existing.changeApplicationStatus(ApplicationStatus.SUBMITTED);
            return existing;
        }).orElseGet(() -> {
            User applicant = entityManager.getReference(User.class, applicantId);
            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers(command.answersJson())
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            return applicationRepository.save(application);
        });
    }

    public Application temporarySaveApplication(Long clubId, Long postingId, Long applicantId, SubmitApplicationCommand command) {
        RecruitmentPosting posting = findAndValidatePosting(postingId, clubId);
        validatePostingIsAccepting(posting);

        RecruitmentSchema schema = recruitmentSchemaRepository
                .findLatestByPostingIdAndProcessType(postingId, ProcessType.DOCUMENT)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_SCHEMA_NOT_FOUND));

        return applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                postingId, applicantId,
                List.of(ApplicationStatus.SUBMITTED, ApplicationStatus.TEMPORARY)
        ).map(existing -> {
            if (existing.getApplicationStatus() == ApplicationStatus.SUBMITTED) {
                throw new BusinessException(RecruitmentErrorCode.APPLICATION_ALREADY_SUBMITTED);
            }
            // 기존 TEMPORARY 갱신
            existing.updateAnswers(command.answersJson());
            return existing;
        }).orElseGet(() -> {
            User applicant = entityManager.getReference(User.class, applicantId);
            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers(command.answersJson())
                    .applicationStatus(ApplicationStatus.TEMPORARY)
                    .build();
            return applicationRepository.save(application);
        });
    }

    public void withdrawApplication(Long postingId, Long applicationId, Long applicantId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getApplicant().getId().equals(applicantId)) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_OWNER);
        }

        ApplicationStatus status = application.getApplicationStatus();
        if (status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_WITHDRAWABLE);
        }

        application.inactivate();
    }

    @Transactional(readOnly = true)
    public List<MyApplicationListResult> getMyApplications(Long applicantId) {
        List<Application> applications = applicationRepository
                .findAllByApplicantIdWithPostingAndClub(applicantId);

        return applications.stream()
                .map(this::toMyApplicationListResult)
                .toList();
    }

    public Application editApplication(Long clubId, Long postingId, Long applicationId, Long applicantId, SubmitApplicationCommand command) {
        RecruitmentPosting posting = findAndValidatePosting(postingId, clubId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));

        if (application.getStatus() != BaseStatus.ACTIVE) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
        }

        if (application.getApplicant() == null || !application.getApplicant().getId().equals(applicantId)) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_OWNER);
        }

        validateApplicationEditable(application);
        validateEditWindow(posting, application);

        application.updateAnswers(command.answersJson());
        return application;
    }

    public Application submitGuestApplication(Long clubId, Long postingId, GuestApplicationCommand command) {
        RecruitmentPosting posting = findAndValidatePosting(postingId, clubId);
        validatePostingIsAccepting(posting);

        RecruitmentSchema schema = recruitmentSchemaRepository
                .findLatestByPostingIdAndProcessType(postingId, ProcessType.DOCUMENT)
                .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_SCHEMA_NOT_FOUND));

        applicationRepository.findByPostingIdAndGuestPhoneAndStatuses(
                postingId, command.guestPhone(),
                List.of(ApplicationStatus.SUBMITTED, ApplicationStatus.TEMPORARY)
        ).ifPresent(existing -> {
            throw new BusinessException(RecruitmentErrorCode.GUEST_APPLICATION_ALREADY_SUBMITTED);
        });

        Application application = Application.builder()
                .recruitmentSchema(schema)
                .guestPhone(command.guestPhone())
                .answers(command.answersJson())
                .applicationStatus(ApplicationStatus.SUBMITTED)
                .build();
        return applicationRepository.save(application);
    }

    public Application editGuestApplication(Long clubId, Long postingId, GuestEditApplicationCommand command) {
        RecruitmentPosting posting = findAndValidatePosting(postingId, clubId);

        Application application = applicationRepository.findByPostingIdAndGuestPhoneAndStatuses(
                postingId, command.guestPhone(),
                List.of(ApplicationStatus.SUBMITTED, ApplicationStatus.TEMPORARY)
        ).orElseThrow(() -> new BusinessException(RecruitmentErrorCode.GUEST_APPLICATION_NOT_FOUND));

        validateApplicationEditable(application);
        validateEditWindow(posting, application);

        application.updateAnswers(command.answersJson());
        return application;
    }

    private void validateApplicationEditable(Application application) {
        ApplicationStatus status = application.getApplicationStatus();
        if (status != ApplicationStatus.SUBMITTED && status != ApplicationStatus.TEMPORARY) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_EDITABLE);
        }
    }

    private void validateEditWindow(RecruitmentPosting posting, Application application) {
        if (posting.getEditWindowDays() == 0) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime deadline;

        if (posting.getEditWindowBasis() == EditWindowBasis.DEPLOYED) {
            deadline = posting.getStartAt().plusDays(posting.getEditWindowDays());
        } else {
            deadline = application.getCreatedAt().plusDays(posting.getEditWindowDays());
        }

        if (now.isAfter(deadline)) {
            throw new BusinessException(RecruitmentErrorCode.APPLICATION_EDIT_WINDOW_CLOSED);
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

    private void validatePostingIsAccepting(RecruitmentPosting posting) {
        if (posting.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_ACCEPTING);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (now.isBefore(posting.getStartAt()) || now.isAfter(posting.getEndAt())) {
            throw new BusinessException(RecruitmentErrorCode.POSTING_NOT_ACCEPTING);
        }
    }

    private MyApplicationListResult toMyApplicationListResult(Application application) {
        RecruitmentPosting posting = application.getRecruitmentSchema()
                .getRecruitmentProcess().getPosting();

        return new MyApplicationListResult(
                application.getId(),
                posting.getId(),
                posting.getClub().getId(),
                posting.getClub().getName(),
                posting.getTitle(),
                application.getApplicationStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    public record SubmitApplicationCommand(
            String answersJson
    ) {
    }

    public record GuestApplicationCommand(
            String guestPhone,
            String answersJson
    ) {
    }

    public record GuestEditApplicationCommand(
            String guestPhone,
            String answersJson
    ) {
    }
}
