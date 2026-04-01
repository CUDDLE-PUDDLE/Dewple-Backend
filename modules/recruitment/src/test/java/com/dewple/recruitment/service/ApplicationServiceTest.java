package com.dewple.recruitment.service;

import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ProcessType;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.entity.RecruitmentPosting;
import com.dewple.recruitment.entity.RecruitmentProcess;
import com.dewple.recruitment.entity.RecruitmentSchema;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.repository.ApplicationRepository;
import com.dewple.recruitment.repository.RecruitmentPostingRepository;
import com.dewple.recruitment.repository.RecruitmentSchemaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RecruitmentPostingRepository recruitmentPostingRepository;

    @Mock
    private RecruitmentSchemaRepository recruitmentSchemaRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ApplicationService applicationService;

    private static final Long CLUB_ID = 1L;
    private static final Long POSTING_ID = 100L;
    private static final Long APPLICANT_ID = 10L;
    private static final Long APPLICATION_ID = 500L;
    private static final Long SCHEMA_ID = 200L;
    private RecruitmentPosting posting;
    private RecruitmentSchema schema;
    private Club club;

    @BeforeEach
    void setUp() {
        club = mock(Club.class);
        lenient().when(club.getId()).thenReturn(CLUB_ID);

        posting = RecruitmentPosting.builder()
                .club(club)
                .title("테스트 공고")
                .recruitmentStatus(RecruitmentStatus.OPEN)
                .recentRecruitmentVersion(1L)
                .startAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .endAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30))
                .hasSecondInterview(false)
                .build();
        ReflectionTestUtils.setField(posting, "id", POSTING_ID);

        RecruitmentProcess process = RecruitmentProcess.builder()
                .posting(posting)
                .processOrder(1)
                .name("서류 접수")
                .processType(ProcessType.DOCUMENT)
                .startAt(posting.getStartAt())
                .endAt(posting.getEndAt())
                .build();

        schema = RecruitmentSchema.builder()
                .recruitmentProcess(process)
                .version(1L)
                .applicationForm("{}")
                .build();
        ReflectionTestUtils.setField(schema, "id", SCHEMA_ID);
    }

    @Nested
    @DisplayName("submitApplication — 지원서 제출")
    class SubmitApplication {

        private final ApplicationService.SubmitApplicationCommand command =
                new ApplicationService.SubmitApplicationCommand("[{\"key\":\"q1\",\"value\":\"답변\"}]");

        @Test
        @DisplayName("성공: 신규 지원서 제출")
        void successNewApplication() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(recruitmentSchemaRepository.findLatestByPostingIdAndProcessType(POSTING_ID, ProcessType.DOCUMENT))
                    .willReturn(Optional.of(schema));
            given(applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                    eq(POSTING_ID), eq(APPLICANT_ID), any()))
                    .willReturn(Optional.empty());

            User applicant = mock(User.class);
            given(entityManager.getReference(User.class, APPLICANT_ID)).willReturn(applicant);

            Application savedApplication = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers(command.answersJson())
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(savedApplication, "id", APPLICATION_ID);
            given(applicationRepository.save(any(Application.class))).willReturn(savedApplication);

            // when
            Application result = applicationService.submitApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command);

            // then
            assertThat(result.getId()).isEqualTo(APPLICATION_ID);
            assertThat(result.getApplicationStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            verify(applicationRepository).save(any(Application.class));
        }

        @Test
        @DisplayName("성공: TEMPORARY 상태 지원서를 SUBMITTED로 전환")
        void successTemporaryToSubmitted() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(recruitmentSchemaRepository.findLatestByPostingIdAndProcessType(POSTING_ID, ProcessType.DOCUMENT))
                    .willReturn(Optional.of(schema));

            Application existingApp = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(mock(User.class))
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.TEMPORARY)
                    .build();
            ReflectionTestUtils.setField(existingApp, "id", APPLICATION_ID);

            given(applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                    eq(POSTING_ID), eq(APPLICANT_ID), any()))
                    .willReturn(Optional.of(existingApp));

            // when
            Application result = applicationService.submitApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command);

            // then
            assertThat(result.getApplicationStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(result.getAnswers()).isEqualTo(command.answersJson());
            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이미 SUBMITTED 상태 지원서 존재")
        void failAlreadySubmitted() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(recruitmentSchemaRepository.findLatestByPostingIdAndProcessType(POSTING_ID, ProcessType.DOCUMENT))
                    .willReturn(Optional.of(schema));

            Application existingApp = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(mock(User.class))
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();

            given(applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                    eq(POSTING_ID), eq(APPLICANT_ID), any()))
                    .willReturn(Optional.of(existingApp));

            // when & then
            assertThatThrownBy(() -> applicationService.submitApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_ALREADY_SUBMITTED);
        }

        @Test
        @DisplayName("실패: 공고가 존재하지 않음")
        void failPostingNotFound() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicationService.submitApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 공고가 OPEN 상태가 아님")
        void failPostingNotAccepting() {
            // given
            RecruitmentPosting closedPosting = RecruitmentPosting.builder()
                    .club(club)
                    .title("마감된 공고")
                    .recruitmentStatus(RecruitmentStatus.CLOSED)
                    .recentRecruitmentVersion(1L)
                    .startAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(30))
                    .endAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                    .hasSecondInterview(false)
                    .build();
            ReflectionTestUtils.setField(closedPosting, "id", POSTING_ID);

            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(closedPosting));

            // when & then
            assertThatThrownBy(() -> applicationService.submitApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_ACCEPTING);
        }

        @Test
        @DisplayName("실패: 지원서 양식(스키마)이 없음")
        void failSchemaNotFound() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(recruitmentSchemaRepository.findLatestByPostingIdAndProcessType(POSTING_ID, ProcessType.DOCUMENT))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicationService.submitApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_SCHEMA_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("temporarySaveApplication — 지원서 임시저장")
    class TemporarySaveApplication {

        private final ApplicationService.SubmitApplicationCommand command =
                new ApplicationService.SubmitApplicationCommand("[{\"key\":\"q1\",\"value\":\"임시답변\"}]");

        @Test
        @DisplayName("성공: 신규 임시저장")
        void successNewTemporary() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(recruitmentSchemaRepository.findLatestByPostingIdAndProcessType(POSTING_ID, ProcessType.DOCUMENT))
                    .willReturn(Optional.of(schema));
            given(applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                    eq(POSTING_ID), eq(APPLICANT_ID), any()))
                    .willReturn(Optional.empty());

            User applicant = mock(User.class);
            given(entityManager.getReference(User.class, APPLICANT_ID)).willReturn(applicant);

            Application savedApplication = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers(command.answersJson())
                    .applicationStatus(ApplicationStatus.TEMPORARY)
                    .build();
            ReflectionTestUtils.setField(savedApplication, "id", APPLICATION_ID);
            given(applicationRepository.save(any(Application.class))).willReturn(savedApplication);

            // when
            Application result = applicationService.temporarySaveApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command);

            // then
            assertThat(result.getApplicationStatus()).isEqualTo(ApplicationStatus.TEMPORARY);
            verify(applicationRepository).save(any(Application.class));
        }

        @Test
        @DisplayName("성공: 기존 TEMPORARY 답변 갱신")
        void successUpdateExistingTemporary() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(recruitmentSchemaRepository.findLatestByPostingIdAndProcessType(POSTING_ID, ProcessType.DOCUMENT))
                    .willReturn(Optional.of(schema));

            Application existingApp = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(mock(User.class))
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.TEMPORARY)
                    .build();
            ReflectionTestUtils.setField(existingApp, "id", APPLICATION_ID);

            given(applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                    eq(POSTING_ID), eq(APPLICANT_ID), any()))
                    .willReturn(Optional.of(existingApp));

            // when
            Application result = applicationService.temporarySaveApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command);

            // then
            assertThat(result.getAnswers()).isEqualTo(command.answersJson());
            assertThat(result.getApplicationStatus()).isEqualTo(ApplicationStatus.TEMPORARY);
            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이미 SUBMITTED 상태 지원서 존재")
        void failAlreadySubmitted() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(recruitmentSchemaRepository.findLatestByPostingIdAndProcessType(POSTING_ID, ProcessType.DOCUMENT))
                    .willReturn(Optional.of(schema));

            Application existingApp = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(mock(User.class))
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();

            given(applicationRepository.findByPostingIdAndApplicantIdAndStatuses(
                    eq(POSTING_ID), eq(APPLICANT_ID), any()))
                    .willReturn(Optional.of(existingApp));

            // when & then
            assertThatThrownBy(() -> applicationService.temporarySaveApplication(CLUB_ID, POSTING_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_ALREADY_SUBMITTED);
        }
    }

    @Nested
    @DisplayName("withdrawApplication — 지원 철회")
    class WithdrawApplication {

        @Test
        @DisplayName("성공: SUBMITTED 상태 지원서 철회")
        void successWithdrawSubmitted() {
            // given
            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            applicationService.withdrawApplication(POSTING_ID, APPLICATION_ID, APPLICANT_ID);

            // then
            assertThat(application.getStatus()).isEqualTo(BaseStatus.INACTIVE);
        }

        @Test
        @DisplayName("성공: TEMPORARY 상태 지원서 철회")
        void successWithdrawTemporary() {
            // given
            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.TEMPORARY)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            applicationService.withdrawApplication(POSTING_ID, APPLICATION_ID, APPLICANT_ID);

            // then
            assertThat(application.getStatus()).isEqualTo(BaseStatus.INACTIVE);
        }

        @Test
        @DisplayName("실패: 지원서가 존재하지 않음")
        void failApplicationNotFound() {
            // given
            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicationService.withdrawApplication(POSTING_ID, APPLICATION_ID, APPLICANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 본인의 지원서가 아님")
        void failNotOwner() {
            // given
            User otherApplicant = mock(User.class);
            given(otherApplicant.getId()).willReturn(999L);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(otherApplicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> applicationService.withdrawApplication(POSTING_ID, APPLICATION_ID, APPLICANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_OWNER);
        }

        @Test
        @DisplayName("실패: ACCEPTED 상태 지원서는 철회 불가")
        void failWithdrawAccepted() {
            // given
            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.ACCEPTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> applicationService.withdrawApplication(POSTING_ID, APPLICATION_ID, APPLICANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_WITHDRAWABLE);
        }

        @Test
        @DisplayName("실패: REJECTED 상태 지원서는 철회 불가")
        void failWithdrawRejected() {
            // given
            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.REJECTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> applicationService.withdrawApplication(POSTING_ID, APPLICATION_ID, APPLICANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_WITHDRAWABLE);
        }
    }

    @Nested
    @DisplayName("getMyApplications — 내 지원 내역 조회")
    class GetMyApplications {

        @Test
        @DisplayName("성공: 지원 내역 반환")
        void successWithApplications() {
            // given
            Club myClub = mock(Club.class);
            given(myClub.getId()).willReturn(CLUB_ID);
            given(myClub.getName()).willReturn("테스트 동아리");

            RecruitmentPosting myPosting = RecruitmentPosting.builder()
                    .club(myClub)
                    .title("테스트 공고")
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .recentRecruitmentVersion(1L)
                    .startAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                    .endAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30))
                    .hasSecondInterview(false)
                    .build();
            ReflectionTestUtils.setField(myPosting, "id", POSTING_ID);

            RecruitmentProcess process = RecruitmentProcess.builder()
                    .posting(myPosting)
                    .processOrder(1)
                    .name("서류 접수")
                    .processType(ProcessType.DOCUMENT)
                    .startAt(myPosting.getStartAt())
                    .endAt(myPosting.getEndAt())
                    .build();

            RecruitmentSchema mySchema = RecruitmentSchema.builder()
                    .recruitmentProcess(process)
                    .version(1L)
                    .applicationForm("{}")
                    .build();

            Application application = Application.builder()
                    .recruitmentSchema(mySchema)
                    .applicant(mock(User.class))
                    .answers("[{\"key\":\"q1\",\"value\":\"답변\"}]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findAllByApplicantIdWithPostingAndClub(APPLICANT_ID))
                    .willReturn(List.of(application));

            // when
            List<MyApplicationListResult> results = applicationService.getMyApplications(APPLICANT_ID);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(results.get(0).clubName()).isEqualTo("테스트 동아리");
            assertThat(results.get(0).postingTitle()).isEqualTo("테스트 공고");
            assertThat(results.get(0).applicationStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        }

        @Test
        @DisplayName("성공: 지원 내역이 없는 경우 빈 리스트 반환")
        void successEmptyList() {
            // given
            given(applicationRepository.findAllByApplicantIdWithPostingAndClub(APPLICANT_ID))
                    .willReturn(List.of());

            // when
            List<MyApplicationListResult> results = applicationService.getMyApplications(APPLICANT_ID);

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("editApplication — 지원서 수정")
    class EditApplication {

        private final ApplicationService.SubmitApplicationCommand command =
                new ApplicationService.SubmitApplicationCommand("[{\"key\":\"q1\",\"value\":\"수정된 답변\"}]");

        @Test
        @DisplayName("성공: SUBMITTED 상태 지원서 수정")
        void successEditSubmitted() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            Application result = applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command);

            // then
            assertThat(result.getAnswers()).isEqualTo(command.answersJson());
        }

        @Test
        @DisplayName("성공: TEMPORARY 상태 지원서 수정")
        void successEditTemporary() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.TEMPORARY)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            Application result = applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command);

            // then
            assertThat(result.getAnswers()).isEqualTo(command.answersJson());
        }

        @Test
        @DisplayName("성공: 지원 마감일 이전 수정")
        void successWithinDeadline() {
            // given — endAt이 미래이므로 수정 가능
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            Application result = applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command);

            // then
            assertThat(result.getAnswers()).isEqualTo(command.answersJson());
        }

        @Test
        @DisplayName("실패: 지원서가 존재하지 않음")
        void failApplicationNotFound() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 본인의 지원서가 아님")
        void failNotOwner() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User otherApplicant = mock(User.class);
            given(otherApplicant.getId()).willReturn(999L);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(otherApplicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_OWNER);
        }

        @Test
        @DisplayName("실패: ACCEPTED 상태 지원서는 수정 불가")
        void failNotEditableAccepted() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.ACCEPTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_EDITABLE);
        }

        @Test
        @DisplayName("실패: REJECTED 상태 지원서는 수정 불가")
        void failNotEditableRejected() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.REJECTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_EDITABLE);
        }

        @Test
        @DisplayName("실패: 지원 마감일 이후 수정 불가")
        void failEditAfterDeadline() {
            // given — endAt이 과거이므로 수정 불가
            RecruitmentPosting expiredPosting = RecruitmentPosting.builder()
                    .club(club)
                    .title("마감된 공고")
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .recentRecruitmentVersion(1L)
                    .startAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(30))
                    .endAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                    .hasSecondInterview(false)
                    .build();
            ReflectionTestUtils.setField(expiredPosting, "id", POSTING_ID);

            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(expiredPosting));

            User applicant = mock(User.class);
            given(applicant.getId()).willReturn(APPLICANT_ID);

            Application application = Application.builder()
                    .recruitmentSchema(schema)
                    .applicant(applicant)
                    .answers("[]")
                    .applicationStatus(ApplicationStatus.SUBMITTED)
                    .build();
            ReflectionTestUtils.setField(application, "id", APPLICATION_ID);

            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() -> applicationService.editApplication(CLUB_ID, POSTING_ID, APPLICATION_ID, APPLICANT_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_EDIT_WINDOW_CLOSED);
        }
    }

}
