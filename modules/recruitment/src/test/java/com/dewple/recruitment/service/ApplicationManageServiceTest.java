package com.dewple.recruitment.service;

import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.EditWindowBasis;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationManageServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RecruitmentPostingRepository recruitmentPostingRepository;

    @InjectMocks
    private ApplicationManageService applicationManageService;

    private static final Long CLUB_ID = 1L;
    private static final Long USER_ID = 99L;
    private static final Long POSTING_ID = 100L;
    private static final Long APPLICATION_ID = 500L;

    private RecruitmentPosting posting;
    private RecruitmentProcess process;
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
                .editWindowBasis(EditWindowBasis.SUBMITTED)
                .editWindowDays(0)
                .isInterviewRequired(false)
                .build();
        ReflectionTestUtils.setField(posting, "id", POSTING_ID);

        process = RecruitmentProcess.builder()
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
        ReflectionTestUtils.setField(schema, "id", 200L);
    }

    private Application createMockApplication(Long id, ApplicationStatus status, User applicant, String guestPhone) {
        Application application = Application.builder()
                .recruitmentSchema(schema)
                .applicant(applicant)
                .guestPhone(guestPhone)
                .answers("[{\"key\":\"q1\",\"value\":\"답변\"}]")
                .applicationStatus(status)
                .build();
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "createdAt", OffsetDateTime.now(ZoneOffset.UTC));
        return application;
    }

    private User createMockUser(Long id, String name, String phone) {
        User user = User.builder()
                .name(name)
                .phone(phone)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("getApplicationList() - 지원자 목록 조회")
    class GetApplicationListTest {

        @Test
        @DisplayName("성공: 전체 지원자 목록 조회")
        void success() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicantUser = createMockUser(10L, "홍길동", "01011112222");
            Application app1 = createMockApplication(501L, ApplicationStatus.SUBMITTED, applicantUser, null);
            Application app2 = createMockApplication(502L, ApplicationStatus.SUBMITTED, null, "01033334444");

            Pageable pageable = PageRequest.of(0, 20);
            Page<Application> page = new PageImpl<>(List.of(app1, app2), pageable, 2);
            given(applicationRepository.searchByPostingId(eq(POSTING_ID), eq(null), eq(null), any()))
                    .willReturn(page);

            // when
            Page<ApplicationListResult> result = applicationManageService.getApplicationList(
                    CLUB_ID, USER_ID, POSTING_ID, null, null, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).applicantName()).isEqualTo("홍길동");
            assertThat(result.getContent().get(0).applicantPhone()).isEqualTo("01011112222");
            assertThat(result.getContent().get(1).applicantName()).isEqualTo("비회원");
            assertThat(result.getContent().get(1).applicantPhone()).isEqualTo("01033334444");
        }

        @Test
        @DisplayName("성공: 상태 필터로 조회")
        void successWithStatusFilter() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicantUser = createMockUser(10L, "홍길동", "01011112222");
            Application app = createMockApplication(501L, ApplicationStatus.ACCEPTED, applicantUser, null);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Application> page = new PageImpl<>(List.of(app), pageable, 1);
            given(applicationRepository.searchByPostingId(eq(POSTING_ID), eq(ApplicationStatus.ACCEPTED), eq(null), any()))
                    .willReturn(page);

            // when
            Page<ApplicationListResult> result = applicationManageService.getApplicationList(
                    CLUB_ID, USER_ID, POSTING_ID, ApplicationStatus.ACCEPTED, null, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).applicationStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("성공: 키워드 검색")
        void successWithKeyword() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicantUser = createMockUser(10L, "홍길동", "01011112222");
            Application app = createMockApplication(501L, ApplicationStatus.SUBMITTED, applicantUser, null);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Application> page = new PageImpl<>(List.of(app), pageable, 1);
            given(applicationRepository.searchByPostingId(eq(POSTING_ID), eq(null), eq("홍길동"), any()))
                    .willReturn(page);

            // when
            Page<ApplicationListResult> result = applicationManageService.getApplicationList(
                    CLUB_ID, USER_ID, POSTING_ID, null, "홍길동", pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).applicantName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("성공: 빈 결과")
        void successEmptyResult() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            Pageable pageable = PageRequest.of(0, 20);
            Page<Application> page = new PageImpl<>(List.of(), pageable, 0);
            given(applicationRepository.searchByPostingId(eq(POSTING_ID), eq(null), eq(null), any()))
                    .willReturn(page);

            // when
            Page<ApplicationListResult> result = applicationManageService.getApplicationList(
                    CLUB_ID, USER_ID, POSTING_ID, null, null, pageable);

            // then
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("실패: 공고를 찾을 수 없음")
        void failPostingNotFound() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.empty());

            Pageable pageable = PageRequest.of(0, 20);

            // when & then
            assertThatThrownBy(() -> applicationManageService.getApplicationList(
                    CLUB_ID, USER_ID, POSTING_ID, null, null, pageable))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getApplicationDetail() - 지원자 상세 조회")
    class GetApplicationDetailTest {

        @Test
        @DisplayName("성공: 지원자 상세 조회")
        void success() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicantUser = createMockUser(10L, "홍길동", "01011112222");
            Application application = createMockApplication(APPLICATION_ID, ApplicationStatus.SUBMITTED, applicantUser, null);
            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            ApplicationDetailResult result = applicationManageService.getApplicationDetail(
                    CLUB_ID, USER_ID, POSTING_ID, APPLICATION_ID);

            // then
            assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(result.applicantName()).isEqualTo("홍길동");
            assertThat(result.applicantPhone()).isEqualTo("01011112222");
            assertThat(result.answers()).isNotNull();
            assertThat(result.applicationStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        }

        @Test
        @DisplayName("실패: 지원서를 찾을 수 없음")
        void failNotFound() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicationManageService.getApplicationDetail(
                    CLUB_ID, USER_ID, POSTING_ID, APPLICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("changeApplicationStatus() - 단건 상태 변경")
    class ChangeApplicationStatusTest {

        @Test
        @DisplayName("성공: 합격 처리")
        void successAccepted() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicantUser = createMockUser(10L, "홍길동", "01011112222");
            Application application = createMockApplication(APPLICATION_ID, ApplicationStatus.SUBMITTED, applicantUser, null);
            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            applicationManageService.changeApplicationStatus(
                    CLUB_ID, USER_ID, POSTING_ID, APPLICATION_ID, ApplicationStatus.ACCEPTED);

            // then
            assertThat(application.getApplicationStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("성공: 불합격 처리")
        void successRejected() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User applicantUser = createMockUser(10L, "홍길동", "01011112222");
            Application application = createMockApplication(APPLICATION_ID, ApplicationStatus.SUBMITTED, applicantUser, null);
            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

            // when
            applicationManageService.changeApplicationStatus(
                    CLUB_ID, USER_ID, POSTING_ID, APPLICATION_ID, ApplicationStatus.REJECTED);

            // then
            assertThat(application.getApplicationStatus()).isEqualTo(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("실패: 지원서를 찾을 수 없음")
        void failNotFound() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));
            given(applicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applicationManageService.changeApplicationStatus(
                    CLUB_ID, USER_ID, POSTING_ID, APPLICATION_ID, ApplicationStatus.ACCEPTED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("batchChangeApplicationStatus() - 일괄 상태 변경")
    class BatchChangeApplicationStatusTest {

        @Test
        @DisplayName("성공: 복수 건 합격 처리")
        void success() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User user1 = createMockUser(10L, "홍길동", "01011112222");
            User user2 = createMockUser(11L, "김철수", "01033334444");
            Application app1 = createMockApplication(501L, ApplicationStatus.SUBMITTED, user1, null);
            Application app2 = createMockApplication(502L, ApplicationStatus.SUBMITTED, user2, null);

            given(applicationRepository.findAllById(List.of(501L, 502L)))
                    .willReturn(List.of(app1, app2));

            // when
            applicationManageService.batchChangeApplicationStatus(
                    CLUB_ID, USER_ID, POSTING_ID, List.of(501L, 502L), ApplicationStatus.ACCEPTED);

            // then
            assertThat(app1.getApplicationStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
            assertThat(app2.getApplicationStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("실패: 일부 지원서를 찾을 수 없음")
        void failSomeNotFound() {
            // given
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            User user1 = createMockUser(10L, "홍길동", "01011112222");
            Application app1 = createMockApplication(501L, ApplicationStatus.SUBMITTED, user1, null);

            given(applicationRepository.findAllById(List.of(501L, 999L)))
                    .willReturn(List.of(app1));

            // when & then
            assertThatThrownBy(() -> applicationManageService.batchChangeApplicationStatus(
                    CLUB_ID, USER_ID, POSTING_ID, List.of(501L, 999L), ApplicationStatus.ACCEPTED))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(RecruitmentErrorCode.APPLICATION_SOME_NOT_FOUND);
        }
    }
}
