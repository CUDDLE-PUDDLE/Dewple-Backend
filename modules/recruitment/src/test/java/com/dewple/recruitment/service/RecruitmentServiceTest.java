package com.dewple.recruitment.service;

import com.dewple.club.entity.ClubDepartment;
import com.dewple.club.entity.ClubGeneration;
import com.dewple.club.repository.ClubDepartmentRepository;
import com.dewple.club.repository.ClubGenerationRepository;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ProcessType;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.entity.RecruitmentPosting;
import com.dewple.recruitment.entity.RecruitmentProcess;
import com.dewple.recruitment.entity.RecruitmentSchema;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.recruitment.repository.RecruitmentPostingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock
    private RecruitmentPostingRepository recruitmentPostingRepository;

    @Mock
    private ClubDepartmentRepository clubDepartmentRepository;

    @Mock
    private ClubGenerationRepository clubGenerationRepository;

    @Mock
    private EntityManager entityManager;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RecruitmentService recruitmentService;

    private static final Long CLUB_ID = 1L;
    private static final Long CREATOR_ID = 10L;
    private static final Long POSTING_ID = 100L;
    private static final Long DEPARTMENT_ID_1 = 201L;
    private static final Long DEPARTMENT_ID_2 = 202L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private Club club;
    private User creator;
    private ClubGeneration generation;
    private ClubDepartment department1;
    private ClubDepartment department2;

    @BeforeEach
    void setUp() {
        club = mock(Club.class);
        creator = mock(User.class);

        generation = ClubGeneration.builder()
                .club(club)
                .generationNo(1)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();
        ReflectionTestUtils.setField(generation, "id", 301L);

        department1 = ClubDepartment.builder()
                .club(club)
                .name("개발팀")
                .build();
        ReflectionTestUtils.setField(department1, "id", DEPARTMENT_ID_1);

        department2 = ClubDepartment.builder()
                .club(club)
                .name("디자인팀")
                .build();
        ReflectionTestUtils.setField(department2, "id", DEPARTMENT_ID_2);
    }

    private RecruitmentService.CreateRecruitmentCommand createDefaultCommand() {
        return new RecruitmentService.CreateRecruitmentCommand(
                "모집 공고 제목",
                1,
                "[{\"orderNumber\":1,\"text\":\"공고 본문입니다.\"}]",
                List.of(new RecruitmentService.CreateRecruitmentCommand.DepartmentInfo(DEPARTMENT_ID_1, 5)),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 5),
                LocalDate.of(2026, 8, 31),
                false,
                null,
                null,
                null,
                null,
                "{\"textarea\":[],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}"
        );
    }

    private RecruitmentService.CreateRecruitmentCommand createCommandWithInterview() {
        return new RecruitmentService.CreateRecruitmentCommand(
                "면접 포함 공고",
                1,
                "[{\"orderNumber\":1,\"text\":\"면접 포함 공고입니다.\"}]",
                List.of(
                        new RecruitmentService.CreateRecruitmentCommand.DepartmentInfo(DEPARTMENT_ID_1, 3),
                        new RecruitmentService.CreateRecruitmentCommand.DepartmentInfo(DEPARTMENT_ID_2, 2)
                ),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 8, 31),
                true,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "{\"textarea\":[],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}"
        );
    }

    private RecruitmentService.CreateRecruitmentCommand createCommandWithTwoDepartments() {
        return new RecruitmentService.CreateRecruitmentCommand(
                "2개 부서 공고",
                1,
                "[{\"orderNumber\":1,\"text\":\"본문입니다.\"}]",
                List.of(
                        new RecruitmentService.CreateRecruitmentCommand.DepartmentInfo(DEPARTMENT_ID_1, 3),
                        new RecruitmentService.CreateRecruitmentCommand.DepartmentInfo(DEPARTMENT_ID_2, 2)
                ),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 5),
                LocalDate.of(2026, 8, 31),
                false,
                null,
                null,
                null,
                null,
                "{\"textarea\":[],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}"
        );
    }

    private void stubCommonDependencies() {
        given(entityManager.getReference(Club.class, CLUB_ID)).willReturn(club);
        given(entityManager.getReference(User.class, CREATOR_ID)).willReturn(creator);
        given(clubGenerationRepository.findByClubIdAndGenerationNo(CLUB_ID, 1)).willReturn(Optional.of(generation));
        given(clubDepartmentRepository.findByIdAndClubId(DEPARTMENT_ID_1, CLUB_ID)).willReturn(Optional.of(department1));
    }

    private RecruitmentPosting createPostingWithStatus(RecruitmentStatus status, Long version) {
        RecruitmentPosting posting = RecruitmentPosting.builder()
                .club(club)
                .generation(generation)
                .creator(creator)
                .title("기존 공고")
                .content("[{\"orderNumber\":1,\"text\":\"기존 본문\"}]")
                .editWindowBasis(com.dewple.common.enums.EditWindowBasis.SUBMITTED)
                .editWindowDays(0)
                .capacity(5)
                .recruitmentStatus(status)
                .recentRecruitmentVersion(version)
                .startAt(toUtcStartOfDay(LocalDate.of(2026, 3, 1)))
                .endAt(toUtcEndOfDay(LocalDate.of(2026, 3, 31)))
                .resultDate(LocalDate.of(2026, 4, 5))
                .endOfGenerationDate(LocalDate.of(2026, 8, 31))
                .isInterviewRequired(false)
                .build();
        ReflectionTestUtils.setField(posting, "id", POSTING_ID);
        return posting;
    }

    private RecruitmentPosting createOpenPostingWithSchema(String applicationFormJson) {
        RecruitmentPosting posting = createPostingWithStatus(RecruitmentStatus.OPEN, 1L);

        RecruitmentProcess documentProcess = RecruitmentProcess.builder()
                .posting(posting)
                .processOrder(1)
                .name("서류 접수")
                .processType(ProcessType.DOCUMENT)
                .startAt(posting.getStartAt())
                .endAt(posting.getEndAt())
                .build();
        posting.addRecruitmentProcess(documentProcess);

        RecruitmentSchema schema = RecruitmentSchema.builder()
                .recruitmentProcess(documentProcess)
                .version(1L)
                .applicationForm(applicationFormJson)
                .build();
        documentProcess.addRecruitmentSchema(schema);

        return posting;
    }

    // ========== createRecruitment ==========

    @Nested
    @DisplayName("createRecruitment - 공고 생성 (발행)")
    class CreateRecruitment {

        @Test
        @DisplayName("성공: 단일 부서 공고 생성")
        void success_singleDepartment() {
            // given - 공통 의존성 stub 및 단일 부서 커맨드 준비
            stubCommonDependencies();
            given(recruitmentPostingRepository.save(any(RecruitmentPosting.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            RecruitmentService.CreateRecruitmentCommand command = createDefaultCommand();

            // when - 공고 생성 (발행)
            recruitmentService.createRecruitment(CLUB_ID, CREATOR_ID, command);

            // then - OPEN 상태, 버전 1, 부서 1개, 서류 접수 프로세스 1개로 저장됨
            ArgumentCaptor<RecruitmentPosting> captor = ArgumentCaptor.forClass(RecruitmentPosting.class);
            verify(recruitmentPostingRepository).save(captor.capture());

            RecruitmentPosting saved = captor.getValue();
            assertThat(saved.getTitle()).isEqualTo("모집 공고 제목");
            assertThat(saved.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.OPEN);
            assertThat(saved.getRecentRecruitmentVersion()).isEqualTo(1L);
            assertThat(saved.getCapacity()).isEqualTo(5);
            assertThat(saved.getRecruitmentDepartments()).hasSize(1);
            assertThat(saved.getRecruitmentProcesses()).hasSize(1);
            assertThat(saved.getRecruitmentProcesses().get(0).getProcessType()).isEqualTo(ProcessType.DOCUMENT);
        }

        @Test
        @DisplayName("성공: 면접 포함 공고 생성 시 면접 프로세스 및 자동 컴포넌트 추가")
        void success_withInterview() {
            // given - 2개 부서 + 면접 필수 커맨드 준비
            stubCommonDependencies();
            given(clubDepartmentRepository.findByIdAndClubId(DEPARTMENT_ID_2, CLUB_ID)).willReturn(Optional.of(department2));
            given(recruitmentPostingRepository.save(any(RecruitmentPosting.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            RecruitmentService.CreateRecruitmentCommand command = createCommandWithInterview();

            // when - 공고 생성 (발행)
            recruitmentService.createRecruitment(CLUB_ID, CREATOR_ID, command);

            // then - 면접 프로세스 추가, 부서 선택 + 면접 일정 자동 컴포넌트 주입됨
            ArgumentCaptor<RecruitmentPosting> captor = ArgumentCaptor.forClass(RecruitmentPosting.class);
            verify(recruitmentPostingRepository).save(captor.capture());

            RecruitmentPosting saved = captor.getValue();
            assertThat(saved.getRecruitmentProcesses()).hasSize(2);
            assertThat(saved.getRecruitmentProcesses().get(1).getProcessType()).isEqualTo(ProcessType.INTERVIEW);
            assertThat(saved.getCapacity()).isEqualTo(5);
            assertThat(saved.getRecruitmentDepartments()).hasSize(2);

            RecruitmentSchema schema = saved.getRecruitmentProcesses().get(0).getRecruitmentSchemas().get(0);
            assertThat(schema.getApplicationForm()).contains("__auto_department_select_");
            assertThat(schema.getApplicationForm()).contains("__auto_interview_schedule_");
        }

        @Test
        @DisplayName("성공: 2개 이상 부서일 때 부서 선택 자동 컴포넌트 주입")
        void success_multiDepartmentAutoComponent() {
            // given - 2개 부서 (면접 없음) 커맨드 준비
            stubCommonDependencies();
            given(clubDepartmentRepository.findByIdAndClubId(DEPARTMENT_ID_2, CLUB_ID)).willReturn(Optional.of(department2));
            given(recruitmentPostingRepository.save(any(RecruitmentPosting.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            RecruitmentService.CreateRecruitmentCommand command = createCommandWithTwoDepartments();

            // when - 공고 생성 (발행)
            recruitmentService.createRecruitment(CLUB_ID, CREATOR_ID, command);

            // then - 부서 선택 자동 컴포넌트에 개발팀/디자인팀 옵션이 포함됨
            ArgumentCaptor<RecruitmentPosting> captor = ArgumentCaptor.forClass(RecruitmentPosting.class);
            verify(recruitmentPostingRepository).save(captor.capture());

            RecruitmentPosting saved = captor.getValue();
            RecruitmentSchema schema = saved.getRecruitmentProcesses().get(0).getRecruitmentSchemas().get(0);
            assertThat(schema.getApplicationForm()).contains("__auto_department_select_");
            assertThat(schema.getApplicationForm()).contains("개발팀");
            assertThat(schema.getApplicationForm()).contains("디자인팀");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 기수")
        void fail_generationNotFound() {
            // given - 기수 조회 결과 없음
            given(entityManager.getReference(Club.class, CLUB_ID)).willReturn(club);
            given(entityManager.getReference(User.class, CREATOR_ID)).willReturn(creator);
            given(clubGenerationRepository.findByClubIdAndGenerationNo(CLUB_ID, 1)).willReturn(Optional.empty());

            RecruitmentService.CreateRecruitmentCommand command = createDefaultCommand();

            // when - 공고 생성 시도
            // then - GENERATION_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.createRecruitment(CLUB_ID, CREATOR_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.GENERATION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 부서")
        void fail_departmentNotFound() {
            // given - 부서 조회 결과 없음
            given(entityManager.getReference(Club.class, CLUB_ID)).willReturn(club);
            given(entityManager.getReference(User.class, CREATOR_ID)).willReturn(creator);
            given(clubGenerationRepository.findByClubIdAndGenerationNo(CLUB_ID, 1)).willReturn(Optional.of(generation));
            given(clubDepartmentRepository.findByIdAndClubId(DEPARTMENT_ID_1, CLUB_ID)).willReturn(Optional.empty());

            RecruitmentService.CreateRecruitmentCommand command = createDefaultCommand();

            // when - 공고 생성 시도
            // then - DEPARTMENT_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.createRecruitment(CLUB_ID, CREATOR_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.DEPARTMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 종료일이 시작일 이전")
        void fail_invalidDateRange() {
            // given - endDate(3/1)가 startDate(3/31)보다 이전인 커맨드 준비
            given(entityManager.getReference(Club.class, CLUB_ID)).willReturn(club);
            given(entityManager.getReference(User.class, CREATOR_ID)).willReturn(creator);
            given(clubGenerationRepository.findByClubIdAndGenerationNo(CLUB_ID, 1)).willReturn(Optional.of(generation));

            RecruitmentService.CreateRecruitmentCommand command = new RecruitmentService.CreateRecruitmentCommand(
                    "제목", 1, "[{\"text\":\"본문\"}]",
                    List.of(new RecruitmentService.CreateRecruitmentCommand.DepartmentInfo(DEPARTMENT_ID_1, 5)),
                    LocalDate.of(2026, 3, 31), // startDate
                    LocalDate.of(2026, 3, 1),  // endDate (시작 이전)
                    LocalDate.of(2026, 4, 5),
                    LocalDate.of(2026, 8, 31),
                    false, null, null, null, null,
                    "{\"textarea\":[],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}"
            );

            // when - 공고 생성 시도
            // then - INVALID_DATE_RANGE 예외 발생
            assertThatThrownBy(() -> recruitmentService.createRecruitment(CLUB_ID, CREATOR_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("실패: 면접 필수인데 면접 일정 없음")
        void fail_interviewRequiredButNoSchedule() {
            // given - 면접 필수(true)이지만 면접 일정이 null인 커맨드 준비
            given(entityManager.getReference(Club.class, CLUB_ID)).willReturn(club);
            given(entityManager.getReference(User.class, CREATOR_ID)).willReturn(creator);
            given(clubGenerationRepository.findByClubIdAndGenerationNo(CLUB_ID, 1)).willReturn(Optional.of(generation));

            RecruitmentService.CreateRecruitmentCommand command = new RecruitmentService.CreateRecruitmentCommand(
                    "제목", 1, "[{\"text\":\"본문\"}]",
                    List.of(new RecruitmentService.CreateRecruitmentCommand.DepartmentInfo(DEPARTMENT_ID_1, 5)),
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31),
                    LocalDate.of(2026, 4, 5),
                    LocalDate.of(2026, 8, 31),
                    true,
                    null, null, // 면접 일정 없음
                    null, null,
                    "{\"textarea\":[],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}"
            );

            // when - 공고 생성 시도
            // then - INVALID_INTERVIEW_SETTING 예외 발생
            assertThatThrownBy(() -> recruitmentService.createRecruitment(CLUB_ID, CREATOR_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.INVALID_INTERVIEW_SETTING);
        }
    }

    // ========== temporaryStorageRecruitment ==========

    @Nested
    @DisplayName("temporaryStorageRecruitment - 임시 저장")
    class TemporaryStorageRecruitment {

        @Test
        @DisplayName("성공: 새 초안 생성 (postingId == null)")
        void success_newDraft() {
            // given - 공통 의존성 stub, postingId는 null
            stubCommonDependencies();
            given(recruitmentPostingRepository.save(any(RecruitmentPosting.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            RecruitmentService.CreateRecruitmentCommand command = createDefaultCommand();

            // when - postingId null로 임시 저장
            recruitmentService.temporaryStorageRecruitment(CLUB_ID, CREATOR_ID, null, command);

            // then - DRAFT 상태, 버전 0으로 새 공고 저장됨
            ArgumentCaptor<RecruitmentPosting> captor = ArgumentCaptor.forClass(RecruitmentPosting.class);
            verify(recruitmentPostingRepository).save(captor.capture());

            RecruitmentPosting saved = captor.getValue();
            assertThat(saved.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.DRAFT);
            assertThat(saved.getRecentRecruitmentVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("성공: 기존 초안 업데이트 (postingId != null)")
        void success_updateExistingDraft() {
            // given - DRAFT 상태의 기존 공고 및 업데이트 커맨드 준비
            RecruitmentPosting existingPosting = createPostingWithStatus(RecruitmentStatus.DRAFT, 0L);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(existingPosting));
            given(clubGenerationRepository.findByClubIdAndGenerationNo(CLUB_ID, 1)).willReturn(Optional.of(generation));
            given(clubDepartmentRepository.findByIdAndClubId(DEPARTMENT_ID_1, CLUB_ID)).willReturn(Optional.of(department1));

            RecruitmentService.CreateRecruitmentCommand command = createDefaultCommand();

            // when - 기존 postingId로 임시 저장
            recruitmentService.temporaryStorageRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID, command);

            // then - 기존 공고의 필드가 새 커맨드 값으로 업데이트됨
            assertThat(existingPosting.getTitle()).isEqualTo("모집 공고 제목");
            assertThat(existingPosting.getRecruitmentDepartments()).hasSize(1);
            assertThat(existingPosting.getRecruitmentProcesses()).hasSize(1);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고")
        void fail_postingNotFound() {
            // given - 공고 조회 결과 없음
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.empty());

            RecruitmentService.CreateRecruitmentCommand command = createDefaultCommand();

            // when - 존재하지 않는 postingId로 임시 저장 시도
            // then - POSTING_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.temporaryStorageRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: DRAFT가 아닌 공고에 임시 저장 시도")
        void fail_postingNotDraft() {
            // given - OPEN 상태의 공고 준비
            RecruitmentPosting openPosting = createPostingWithStatus(RecruitmentStatus.OPEN, 1L);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(openPosting));

            RecruitmentService.CreateRecruitmentCommand command = createDefaultCommand();

            // when - OPEN 상태 공고에 임시 저장 시도
            // then - POSTING_NOT_DRAFT 예외 발생
            assertThatThrownBy(() -> recruitmentService.temporaryStorageRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_DRAFT);
        }
    }

    // ========== publishRecruitment ==========

    @Nested
    @DisplayName("publishRecruitment - 공고 발행")
    class PublishRecruitment {

        @Test
        @DisplayName("성공: DRAFT 공고를 OPEN으로 발행")
        void success() {
            // given - DRAFT 상태(버전 0)의 공고 + 서류 접수 프로세스 + 스키마(버전 0) 준비
            RecruitmentPosting draftPosting = createPostingWithStatus(RecruitmentStatus.DRAFT, 0L);

            RecruitmentProcess documentProcess = RecruitmentProcess.builder()
                    .posting(draftPosting)
                    .processOrder(1)
                    .name("서류 접수")
                    .processType(ProcessType.DOCUMENT)
                    .startAt(draftPosting.getStartAt())
                    .endAt(draftPosting.getEndAt())
                    .build();
            draftPosting.addRecruitmentProcess(documentProcess);

            RecruitmentSchema schema = RecruitmentSchema.builder()
                    .recruitmentProcess(documentProcess)
                    .version(0L)
                    .applicationForm("{}")
                    .build();
            documentProcess.addRecruitmentSchema(schema);

            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(draftPosting));

            // when - 공고 발행
            recruitmentService.publishRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID);

            // then - 상태 OPEN, 공고 버전 1, 스키마 버전 1로 갱신됨
            assertThat(draftPosting.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.OPEN);
            assertThat(draftPosting.getRecentRecruitmentVersion()).isEqualTo(1L);
            assertThat(schema.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고")
        void fail_postingNotFound() {
            // given - 공고 조회 결과 없음
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.empty());

            // when - 존재하지 않는 공고 발행 시도
            // then - POSTING_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.publishRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: DRAFT가 아닌 공고 발행 시도")
        void fail_postingNotDraft() {
            // given - OPEN 상태의 공고 준비
            RecruitmentPosting openPosting = createPostingWithStatus(RecruitmentStatus.OPEN, 1L);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(openPosting));

            // when - 이미 OPEN 상태인 공고 발행 시도
            // then - POSTING_NOT_DRAFT 예외 발생
            assertThatThrownBy(() -> recruitmentService.publishRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_DRAFT);
        }
    }

    // ========== updateRecruitment ==========

    @Nested
    @DisplayName("updateRecruitment - 공고 수정")
    class UpdateRecruitment {

        private static final String EXISTING_FORM = "{\"textarea\":[{\"key\":\"key1\",\"question\":\"자기소개\"}],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}";

        @Test
        @DisplayName("성공: 제목, 본문, 지원서 양식 수정 및 버전 증가")
        void success() {
            // given - OPEN 상태(버전 1)의 공고 + 기존 key1을 유지하면서 key2를 추가한 새 양식 준비
            String newForm = "{\"textarea\":[{\"key\":\"key1\",\"question\":\"자기소개 수정\"},{\"key\":\"key2\",\"question\":\"지원동기\"}],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}";
            RecruitmentPosting posting = createOpenPostingWithSchema(EXISTING_FORM);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            RecruitmentService.UpdateRecruitmentCommand command = new RecruitmentService.UpdateRecruitmentCommand(
                    "수정된 제목",
                    "[{\"orderNumber\":1,\"text\":\"수정된 본문\"}]",
                    newForm
            );

            // when - 공고 수정
            recruitmentService.updateRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID, command);

            // then - 제목/본문 업데이트, 버전 1→2 증가, 새 스키마(버전 2) 추가됨
            assertThat(posting.getTitle()).isEqualTo("수정된 제목");
            assertThat(posting.getContent()).isEqualTo("[{\"orderNumber\":1,\"text\":\"수정된 본문\"}]");
            assertThat(posting.getRecentRecruitmentVersion()).isEqualTo(2L);

            RecruitmentProcess documentProcess = posting.getRecruitmentProcesses().get(0);
            assertThat(documentProcess.getRecruitmentSchemas()).hasSize(2);

            RecruitmentSchema newSchema = documentProcess.getRecruitmentSchemas().get(1);
            assertThat(newSchema.getVersion()).isEqualTo(2L);
            assertThat(newSchema.getApplicationForm()).isEqualTo(newForm);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고")
        void fail_postingNotFound() {
            // given - 공고 조회 결과 없음
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.empty());

            RecruitmentService.UpdateRecruitmentCommand command = new RecruitmentService.UpdateRecruitmentCommand(
                    "제목", "본문", "{}"
            );

            // when - 존재하지 않는 공고 수정 시도
            // then - POSTING_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.updateRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: OPEN 상태가 아닌 공고 수정 시도")
        void fail_postingNotOpen() {
            // given - DRAFT 상태의 공고 준비
            RecruitmentPosting draftPosting = createPostingWithStatus(RecruitmentStatus.DRAFT, 0L);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(draftPosting));

            RecruitmentService.UpdateRecruitmentCommand command = new RecruitmentService.UpdateRecruitmentCommand(
                    "제목", "본문", "{}"
            );

            // when - DRAFT 상태 공고 수정 시도
            // then - POSTING_NOT_OPEN 예외 발생
            assertThatThrownBy(() -> recruitmentService.updateRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_OPEN);
        }

        @Test
        @DisplayName("실패: 기존 컴포넌트를 삭제한 지원서 양식")
        void fail_formComponentRemoved() {
            // given - 기존 양식에 key1이 있는 공고 + key1이 빠진 새 양식 준비
            RecruitmentPosting posting = createOpenPostingWithSchema(EXISTING_FORM);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            String newFormWithRemovedKey = "{\"textarea\":[],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}";

            RecruitmentService.UpdateRecruitmentCommand command = new RecruitmentService.UpdateRecruitmentCommand(
                    "제목", "본문", newFormWithRemovedKey
            );

            // when - 기존 컴포넌트(key1)가 삭제된 양식으로 수정 시도
            // then - FORM_COMPONENT_REMOVAL_NOT_ALLOWED 예외 발생
            assertThatThrownBy(() -> recruitmentService.updateRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.FORM_COMPONENT_REMOVAL_NOT_ALLOWED);
        }
    }

    // ========== closeRecruitment ==========

    @Nested
    @DisplayName("closeRecruitment - 공고 조기 마감")
    class CloseRecruitment {

        @Test
        @DisplayName("성공: OPEN 상태이고 마감일이 미래인 공고 조기 마감")
        void success() {
            // given - OPEN 상태이고 마감일(2027-12-31)이 미래인 공고 준비
            RecruitmentPosting posting = RecruitmentPosting.builder()
                    .club(club)
                    .generation(generation)
                    .creator(creator)
                    .title("공고")
                    .content("[{\"text\":\"본문\"}]")
                    .editWindowBasis(com.dewple.common.enums.EditWindowBasis.SUBMITTED)
                    .editWindowDays(0)
                    .capacity(5)
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .recentRecruitmentVersion(1L)
                    .startAt(toUtcStartOfDay(LocalDate.of(2026, 1, 1)))
                    .endAt(toUtcEndOfDay(LocalDate.of(2027, 12, 31)))
                    .resultDate(LocalDate.of(2028, 1, 5))
                    .endOfGenerationDate(LocalDate.of(2028, 8, 31))
                    .isInterviewRequired(false)
                    .build();
            ReflectionTestUtils.setField(posting, "id", POSTING_ID);

            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            // when - 공고 조기 마감
            recruitmentService.closeRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID);

            // then - 상태가 CLOSED로 변경됨
            assertThat(posting.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.CLOSED);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고")
        void fail_postingNotFound() {
            // given - 공고 조회 결과 없음
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.empty());

            // when - 존재하지 않는 공고 마감 시도
            // then - POSTING_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.closeRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: OPEN 상태가 아닌 공고 마감 시도")
        void fail_alreadyClosed() {
            // given - CLOSED 상태의 공고 준비
            RecruitmentPosting closedPosting = createPostingWithStatus(RecruitmentStatus.CLOSED, 1L);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(closedPosting));

            // when - 이미 마감된 공고 마감 시도
            // then - POSTING_ALREADY_CLOSED 예외 발생
            assertThatThrownBy(() -> recruitmentService.closeRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_ALREADY_CLOSED);
        }

        @Test
        @DisplayName("실패: 마감일이 이미 지난 공고 조기 마감 시도")
        void fail_alreadyExpired() {
            // given - OPEN 상태이지만 마감일(2024-03-31)이 이미 지난 공고 준비
            RecruitmentPosting posting = RecruitmentPosting.builder()
                    .club(club)
                    .generation(generation)
                    .creator(creator)
                    .title("공고")
                    .content("[{\"text\":\"본문\"}]")
                    .editWindowBasis(com.dewple.common.enums.EditWindowBasis.SUBMITTED)
                    .editWindowDays(0)
                    .capacity(5)
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .recentRecruitmentVersion(1L)
                    .startAt(toUtcStartOfDay(LocalDate.of(2024, 1, 1)))
                    .endAt(toUtcEndOfDay(LocalDate.of(2024, 3, 31)))
                    .resultDate(LocalDate.of(2024, 4, 5))
                    .endOfGenerationDate(LocalDate.of(2024, 8, 31))
                    .isInterviewRequired(false)
                    .build();
            ReflectionTestUtils.setField(posting, "id", POSTING_ID);

            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            // when - 마감 기한이 지난 공고 조기 마감 시도
            // then - POSTING_NOT_EXPIRED 예외 발생
            assertThatThrownBy(() -> recruitmentService.closeRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_EXPIRED);
        }
    }

    // ========== getPreviousApplicationForm ==========

    @Nested
    @DisplayName("getPreviousApplicationForm - 이전 공고 지원 양식 불러오기")
    class GetPreviousApplicationForm {

        private static final String APPLICATION_FORM_V1 = "{\"textarea\":[{\"key\":\"k1\",\"question\":\"자기소개\"}],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}";
        private static final String APPLICATION_FORM_V2 = "{\"textarea\":[{\"key\":\"k1\",\"question\":\"자기소개\"},{\"key\":\"k2\",\"question\":\"지원동기\"}],\"choice\":[],\"file\":[],\"calendar\":[],\"when2meet\":[]}";

        @Test
        @DisplayName("성공: 이전 공고의 최신 버전 지원 양식 반환")
        void success() {
            // given - OPEN 상태의 이전 공고 + 서류 접수 프로세스 + 스키마(버전 1) 준비
            RecruitmentPosting posting = createOpenPostingWithSchema(APPLICATION_FORM_V1);
            given(recruitmentPostingRepository.findFirstByClubIdAndRecruitmentStatusNotOrderByCreatedAtDesc(
                    CLUB_ID, RecruitmentStatus.DRAFT))
                    .willReturn(Optional.of(posting));

            // when - 이전 공고 지원 양식 조회
            String result = recruitmentService.getPreviousApplicationForm(CLUB_ID, CREATOR_ID);

            // then - 지원 양식 JSON 반환
            assertThat(result).isEqualTo(APPLICATION_FORM_V1);
        }

        @Test
        @DisplayName("성공: 여러 버전이 있을 때 최신 버전의 지원 양식 반환")
        void success_latestVersion() {
            // given - 서류 접수 프로세스에 버전 1, 2 두 개의 스키마가 있는 공고 준비
            RecruitmentPosting posting = createOpenPostingWithSchema(APPLICATION_FORM_V1);
            RecruitmentProcess documentProcess = posting.getRecruitmentProcesses().get(0);

            RecruitmentSchema schemaV2 = RecruitmentSchema.builder()
                    .recruitmentProcess(documentProcess)
                    .version(2L)
                    .applicationForm(APPLICATION_FORM_V2)
                    .build();
            documentProcess.addRecruitmentSchema(schemaV2);

            given(recruitmentPostingRepository.findFirstByClubIdAndRecruitmentStatusNotOrderByCreatedAtDesc(
                    CLUB_ID, RecruitmentStatus.DRAFT))
                    .willReturn(Optional.of(posting));

            // when - 이전 공고 지원 양식 조회
            String result = recruitmentService.getPreviousApplicationForm(CLUB_ID, CREATOR_ID);

            // then - 최신 버전(v2)의 지원 양식 반환
            assertThat(result).isEqualTo(APPLICATION_FORM_V2);
        }

        @Test
        @DisplayName("성공: 이전 공고가 없으면 null 반환")
        void success_noPreviousPosting() {
            // given - 해당 동아리에 이전 공고 없음
            given(recruitmentPostingRepository.findFirstByClubIdAndRecruitmentStatusNotOrderByCreatedAtDesc(
                    CLUB_ID, RecruitmentStatus.DRAFT))
                    .willReturn(Optional.empty());

            // when - 이전 공고 지원 양식 조회
            String result = recruitmentService.getPreviousApplicationForm(CLUB_ID, CREATOR_ID);

            // then - null 반환
            assertThat(result).isNull();
        }
    }

    // ========== incrementViewCount ==========

    @Nested
    @DisplayName("incrementViewCount - 공고 조회수 증가")
    class IncrementViewCount {

        @Test
        @DisplayName("성공: 조회수 1 증가")
        void success() {
            // given - 업데이트 성공 (1행 변경)
            given(recruitmentPostingRepository.incrementViewCount(POSTING_ID)).willReturn(1);

            // when - 조회수 증가
            recruitmentService.incrementViewCount(POSTING_ID);

            // then - incrementViewCount가 호출됨
            verify(recruitmentPostingRepository).incrementViewCount(POSTING_ID);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고")
        void fail_postingNotFound() {
            // given - 업데이트 실패 (0행 변경)
            given(recruitmentPostingRepository.incrementViewCount(999L)).willReturn(0);

            // when - 존재하지 않는 공고 조회수 증가 시도
            // then - POSTING_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.incrementViewCount(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }
    }

    // ========== deleteRecruitment ==========

    @Nested
    @DisplayName("deleteRecruitment - 공고 삭제 (soft delete)")
    class DeleteRecruitment {

        @Test
        @DisplayName("성공: 공고 soft delete")
        void success() {
            // given - OPEN 상태의 공고 준비
            RecruitmentPosting posting = createPostingWithStatus(RecruitmentStatus.OPEN, 1L);
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.of(posting));

            // when - 공고 삭제
            recruitmentService.deleteRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID);

            // then - BaseStatus가 INACTIVE로 변경됨 (soft delete)
            assertThat(posting.getStatus()).isEqualTo(BaseStatus.INACTIVE);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고")
        void fail_postingNotFound() {
            // given - 공고 조회 결과 없음
            given(recruitmentPostingRepository.findById(POSTING_ID)).willReturn(Optional.empty());

            // when - 존재하지 않는 공고 삭제 시도
            // then - POSTING_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.deleteRecruitment(CLUB_ID, CREATOR_ID, POSTING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }
    }

    // ========== getPublicPostingList ==========

    @Nested
    @DisplayName("getPublicPostingList - 공개 공고 목록 조회")
    class GetPublicPostingList {

        @Test
        @DisplayName("성공: OPEN 상태 공고 목록 조회 (마감 임박순 정렬)")
        void success_openStatus() {
            // given - OPEN 상태의 공고 2개 준비 (endAt이 다른)
            RecruitmentPosting posting1 = createPostingWithStatusAndEndAt(
                    RecruitmentStatus.OPEN, 1L,
                    toUtcEndOfDay(LocalDate.of(2026, 4, 15)));
            ReflectionTestUtils.setField(posting1, "id", 101L);

            RecruitmentPosting posting2 = createPostingWithStatusAndEndAt(
                    RecruitmentStatus.OPEN, 1L,
                    toUtcEndOfDay(LocalDate.of(2026, 3, 31)));
            ReflectionTestUtils.setField(posting2, "id", 102L);

            given(recruitmentPostingRepository.findPostingsForPublicList(
                    eq(CLUB_ID), eq(List.of(RecruitmentStatus.OPEN)), eq(BaseStatus.ACTIVE)))
                    .willReturn(new java.util.ArrayList<>(List.of(posting1, posting2)));

            // when - OPEN 상태 목록 조회
            List<PublicPostingListResult> result = recruitmentService.getPublicPostingList(CLUB_ID, "OPEN");

            // then - 마감 임박순(endAt ASC) 정렬: posting2(3/31)이 먼저
            assertThat(result).hasSize(2);
            assertThat(result.get(0).postingId()).isEqualTo(102L);
            assertThat(result.get(1).postingId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("성공: CLOSED 상태 공고 목록 조회 (최근순 정렬)")
        void success_closedStatus() {
            // given - CLOSED/ARCHIVED 상태의 공고 2개 준비
            RecruitmentPosting posting1 = createPostingWithStatusAndEndAt(
                    RecruitmentStatus.CLOSED, 1L,
                    toUtcEndOfDay(LocalDate.of(2026, 2, 28)));
            ReflectionTestUtils.setField(posting1, "id", 101L);

            RecruitmentPosting posting2 = createPostingWithStatusAndEndAt(
                    RecruitmentStatus.ARCHIVED, 1L,
                    toUtcEndOfDay(LocalDate.of(2026, 3, 15)));
            ReflectionTestUtils.setField(posting2, "id", 102L);

            given(recruitmentPostingRepository.findPostingsForPublicList(
                    eq(CLUB_ID), eq(List.of(RecruitmentStatus.CLOSED, RecruitmentStatus.ARCHIVED)), eq(BaseStatus.ACTIVE)))
                    .willReturn(new java.util.ArrayList<>(List.of(posting1, posting2)));

            // when - CLOSED 상태 목록 조회
            List<PublicPostingListResult> result = recruitmentService.getPublicPostingList(CLUB_ID, "CLOSED");

            // then - 최근순(endAt DESC) 정렬: posting2(3/15)가 먼저
            assertThat(result).hasSize(2);
            assertThat(result.get(0).postingId()).isEqualTo(102L);
            assertThat(result.get(1).postingId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("성공: 결과가 빈 리스트")
        void success_emptyList() {
            // given - 공고 없음
            given(recruitmentPostingRepository.findPostingsForPublicList(
                    eq(CLUB_ID), eq(List.of(RecruitmentStatus.OPEN)), eq(BaseStatus.ACTIVE)))
                    .willReturn(new java.util.ArrayList<>());

            // when - OPEN 상태 목록 조회
            List<PublicPostingListResult> result = recruitmentService.getPublicPostingList(CLUB_ID, "OPEN");

            // then - 빈 리스트 반환
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("실패: 잘못된 status 필터")
        void fail_invalidStatusFilter() {
            // when - 잘못된 status로 목록 조회
            // then - INVALID_STATUS_FILTER 예외 발생
            assertThatThrownBy(() -> recruitmentService.getPublicPostingList(CLUB_ID, "INVALID"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.INVALID_STATUS_FILTER);
        }
    }

    // ========== getPublicPostingDetail ==========

    @Nested
    @DisplayName("getPublicPostingDetail - 공개 공고 상세 조회")
    class GetPublicPostingDetail {

        @Test
        @DisplayName("성공: 공고 상세 조회")
        void success() {
            // given - OPEN 상태의 공고 준비
            RecruitmentPosting posting = createPostingWithStatus(RecruitmentStatus.OPEN, 1L);
            given(recruitmentPostingRepository.findPostingDetailForPublic(POSTING_ID, CLUB_ID, BaseStatus.ACTIVE))
                    .willReturn(Optional.of(posting));

            // when - 공고 상세 조회
            PublicPostingDetailResult result = recruitmentService.getPublicPostingDetail(CLUB_ID, POSTING_ID);

            // then - 조회 결과 반환
            assertThat(result.postingId()).isEqualTo(POSTING_ID);
            assertThat(result.title()).isEqualTo("기존 공고");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고")
        void fail_postingNotFound() {
            // given - 공고 조회 결과 없음
            given(recruitmentPostingRepository.findPostingDetailForPublic(999L, CLUB_ID, BaseStatus.ACTIVE))
                    .willReturn(Optional.empty());

            // when - 존재하지 않는 공고 상세 조회
            // then - POSTING_NOT_FOUND 예외 발생
            assertThatThrownBy(() -> recruitmentService.getPublicPostingDetail(CLUB_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(RecruitmentErrorCode.POSTING_NOT_FOUND);
        }
    }

    // ── Timezone Helper ──

    private static OffsetDateTime toUtcStartOfDay(LocalDate date) {
        return date.atStartOfDay(KST).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    private static OffsetDateTime toUtcEndOfDay(LocalDate date) {
        return date.atTime(23, 59, 59).atZone(KST).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    private RecruitmentPosting createPostingWithStatusAndEndAt(RecruitmentStatus status, Long version, OffsetDateTime endAt) {
        RecruitmentPosting posting = RecruitmentPosting.builder()
                .club(club)
                .generation(generation)
                .creator(creator)
                .title("공고")
                .content("[{\"text\":\"본문\"}]")
                .editWindowBasis(com.dewple.common.enums.EditWindowBasis.SUBMITTED)
                .editWindowDays(0)
                .capacity(5)
                .recruitmentStatus(status)
                .recentRecruitmentVersion(version)
                .startAt(toUtcStartOfDay(LocalDate.of(2026, 1, 1)))
                .endAt(endAt)
                .resultDate(LocalDate.of(2026, 5, 1))
                .endOfGenerationDate(LocalDate.of(2026, 8, 31))
                .isInterviewRequired(false)
                .build();
        return posting;
    }
}
