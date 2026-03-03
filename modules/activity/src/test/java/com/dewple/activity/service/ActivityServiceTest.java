package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.club.entity.ClubMember;
import com.dewple.club.entity.ClubRole;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.club.repository.ClubRepository;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.OpenType;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityParticipantRepository activityParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private ActivityService activityService;

    private static final Long USER_ID = 1L;
    private static final Long CLUB_ID = 10L;
    private static final OffsetDateTime START_AT = OffsetDateTime.now().plusDays(7);
    private static final OffsetDateTime END_AT = OffsetDateTime.now().plusDays(14);

    @Nested
    @DisplayName("createActivity - 모임 생성")
    class CreateActivity {

        @Test
        @DisplayName("성공: 개인 모임 생성")
        void successWithPersonalActivity() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
            given(activityRepository.save(any(Activity.class))).willAnswer(invocation -> {
                Activity saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 100L);
                ReflectionTestUtils.setField(saved, "createdAt", OffsetDateTime.now());
                return saved;
            });

            CreateActivityParam param = new CreateActivityParam(
                    null, OpenType.PUBLIC, "봄맞이 독서 모임", "함께 책을 읽어요",
                    20, false, true, START_AT, END_AT
            );

            // when
            CreateActivityResult result = activityService.createActivity(USER_ID, param);

            // then
            assertThat(result.activityId()).isEqualTo(100L);
            assertThat(result.clubId()).isNull();
            assertThat(result.clubName()).isNull();
            assertThat(result.openType()).isEqualTo(OpenType.PUBLIC);
            assertThat(result.name()).isEqualTo("봄맞이 독서 모임");
            assertThat(result.description()).isEqualTo("함께 책을 읽어요");
            assertThat(result.capacity()).isEqualTo(20);
            assertThat(result.isAttendanceCheck()).isFalse();
            assertThat(result.isSearchable()).isTrue();

            verify(activityRepository).save(any(Activity.class));
        }

        @Test
        @DisplayName("성공: 동아리 모임 생성")
        void successWithClubActivity() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            ClubRole role = ClubRole.builder()
                    .club(club)
                    .name("운영진")
                    .permissions(Permission.MANAGE_ACTIVITY.getValue())
                    .build();

            ClubMember member = ClubMember.builder()
                    .club(club)
                    .user(creator)
                    .role(role)
                    .build();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, USER_ID)).willReturn(Optional.of(member));
            given(activityRepository.save(any(Activity.class))).willAnswer(invocation -> {
                Activity saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 100L);
                ReflectionTestUtils.setField(saved, "createdAt", OffsetDateTime.now());
                return saved;
            });

            CreateActivityParam param = new CreateActivityParam(
                    CLUB_ID, OpenType.PRIVATE, "동아리 정기 모임", "이번 주 정기 모임입니다",
                    null, true, false, START_AT, END_AT
            );

            // when
            CreateActivityResult result = activityService.createActivity(USER_ID, param);

            // then
            assertThat(result.activityId()).isEqualTo(100L);
            assertThat(result.clubId()).isEqualTo(CLUB_ID);
            assertThat(result.clubName()).isEqualTo("테스트 동아리");
            assertThat(result.openType()).isEqualTo(OpenType.PRIVATE);
            assertThat(result.isAttendanceCheck()).isTrue();
            assertThat(result.isSearchable()).isFalse();
            assertThat(result.capacity()).isNull();
        }

        @Test
        @DisplayName("성공: capacity가 null인 모임 (인원 제한 없음)")
        void successWithUnlimitedCapacity() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
            given(activityRepository.save(any(Activity.class))).willAnswer(invocation -> {
                Activity saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 100L);
                ReflectionTestUtils.setField(saved, "createdAt", OffsetDateTime.now());
                return saved;
            });

            CreateActivityParam param = new CreateActivityParam(
                    null, OpenType.PUBLIC, "오픈 모임", "누구나 환영",
                    null, null, null, START_AT, END_AT
            );

            // when
            CreateActivityResult result = activityService.createActivity(USER_ID, param);

            // then
            assertThat(result.capacity()).isNull();
            assertThat(result.isAttendanceCheck()).isFalse();
            assertThat(result.isSearchable()).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failWithUserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            CreateActivityParam param = new CreateActivityParam(
                    null, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, START_AT, END_AT
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(999L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 종료 시간이 시작 시간보다 이전")
        void failWithEndBeforeStart() {
            // given
            User creator = createUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));

            CreateActivityParam param = new CreateActivityParam(
                    null, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, END_AT, START_AT
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_END_BEFORE_START);
                    });
        }

        @Test
        @DisplayName("실패: 종료 시간과 시작 시간이 동일")
        void failWithSameStartAndEnd() {
            // given
            User creator = createUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));

            OffsetDateTime sameTime = OffsetDateTime.now().plusDays(7);
            CreateActivityParam param = new CreateActivityParam(
                    null, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, sameTime, sameTime
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_END_BEFORE_START);
                    });
        }

        @Test
        @DisplayName("실패: 시작 시간이 과거")
        void failWithStartInPast() {
            // given
            User creator = createUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));

            OffsetDateTime pastStart = OffsetDateTime.now().minusDays(1);
            OffsetDateTime futureEnd = OffsetDateTime.now().plusDays(1);
            CreateActivityParam param = new CreateActivityParam(
                    null, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, pastStart, futureEnd
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_START_IN_PAST);
                    });
        }

        @Test
        @DisplayName("실패: 존재하지 않는 동아리")
        void failWithClubNotFound() {
            // given
            User creator = createUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
            given(clubRepository.findById(999L)).willReturn(Optional.empty());

            CreateActivityParam param = new CreateActivityParam(
                    999L, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, START_AT, END_AT
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ClubErrorCode.CLUB_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 동아리 멤버가 아닌 사용자")
        void failWithNotClubMember() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, USER_ID)).willReturn(Optional.empty());

            CreateActivityParam param = new CreateActivityParam(
                    CLUB_ID, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, START_AT, END_AT
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ClubErrorCode.NOT_CLUB_MEMBER);
                    });
        }

        @Test
        @DisplayName("실패: 역할이 없는 멤버 (role이 null)")
        void failWithNoRole() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            ClubMember member = ClubMember.builder()
                    .club(club)
                    .user(creator)
                    .role(null)
                    .build();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, USER_ID)).willReturn(Optional.of(member));

            CreateActivityParam param = new CreateActivityParam(
                    CLUB_ID, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, START_AT, END_AT
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ClubErrorCode.CLUB_PERMISSION_DENIED);
                    });
        }

        @Test
        @DisplayName("실패: MANAGE_ACTIVITY 권한이 없는 멤버")
        void failWithNoPermission() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            ClubRole role = ClubRole.builder()
                    .club(club)
                    .name("문의 답변")
                    .permissions(Permission.ANSWER_INQUIRY.getValue())
                    .build();

            ClubMember member = ClubMember.builder()
                    .club(club)
                    .user(creator)
                    .role(role)
                    .build();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(creator));
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, USER_ID)).willReturn(Optional.of(member));

            CreateActivityParam param = new CreateActivityParam(
                    CLUB_ID, OpenType.PUBLIC, "모임", "설명",
                    10, false, true, START_AT, END_AT
            );

            // when & then
            assertThatThrownBy(() -> activityService.createActivity(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ClubErrorCode.CLUB_PERMISSION_DENIED);
                    });
        }
    }

    @Nested
    @DisplayName("deleteActivity - 모임 삭제")
    class DeleteActivity {

        private static final Long ACTIVITY_ID = 100L;
        private static final Long OTHER_USER_ID = 2L;

        @Test
        @DisplayName("성공: 생성자가 개인 모임 삭제")
        void successDeletePersonalActivityByCreator() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Activity activity = createActivity(creator, null);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));

            // when
            activityService.deleteActivity(USER_ID, ACTIVITY_ID);

            // then
            assertThat(activity.getStatus()).isEqualTo(BaseStatus.INACTIVE);
        }

        @Test
        @DisplayName("성공: 생성자가 동아리 모임 삭제")
        void successDeleteClubActivityByCreator() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            Activity activity = createActivity(creator, club);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));

            // when
            activityService.deleteActivity(USER_ID, ACTIVITY_ID);

            // then
            assertThat(activity.getStatus()).isEqualTo(BaseStatus.INACTIVE);
        }

        @Test
        @DisplayName("성공: MANAGE_ACTIVITY 권한 보유자가 동아리 모임 삭제")
        void successDeleteClubActivityByManager() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            User manager = createUser();
            ReflectionTestUtils.setField(manager, "id", OTHER_USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            Activity activity = createActivity(creator, club);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            ClubRole role = ClubRole.builder()
                    .club(club)
                    .name("운영진")
                    .permissions(Permission.MANAGE_ACTIVITY.getValue())
                    .build();

            ClubMember member = ClubMember.builder()
                    .club(club)
                    .user(manager)
                    .role(role)
                    .build();

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, OTHER_USER_ID)).willReturn(Optional.of(member));

            // when
            activityService.deleteActivity(OTHER_USER_ID, ACTIVITY_ID);

            // then
            assertThat(activity.getStatus()).isEqualTo(BaseStatus.INACTIVE);
        }

        @Test
        @DisplayName("실패: 모임 없음")
        void failWithActivityNotFound() {
            // given
            given(activityRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> activityService.deleteActivity(USER_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 이미 삭제된 모임")
        void failWithAlreadyInactive() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Activity activity = createActivity(creator, null);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
            activity.inactivate();

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));

            // when & then
            assertThatThrownBy(() -> activityService.deleteActivity(USER_ID, ACTIVITY_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_ALREADY_INACTIVE);
                    });
        }

        @Test
        @DisplayName("실패: 개인 모임에서 생성자가 아닌 사용자")
        void failWithNoPermissionForPersonalActivity() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Activity activity = createActivity(creator, null);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));

            // when & then
            assertThatThrownBy(() -> activityService.deleteActivity(OTHER_USER_ID, ACTIVITY_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED);
                    });
        }

        @Test
        @DisplayName("실패: 동아리 모임에서 권한 없는 비생성자")
        void failWithNoPermissionForClubActivity() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            User otherUser = createUser();
            ReflectionTestUtils.setField(otherUser, "id", OTHER_USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            Activity activity = createActivity(creator, club);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            ClubRole role = ClubRole.builder()
                    .club(club)
                    .name("문의 답변")
                    .permissions(Permission.ANSWER_INQUIRY.getValue())
                    .build();

            ClubMember member = ClubMember.builder()
                    .club(club)
                    .user(otherUser)
                    .role(role)
                    .build();

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, OTHER_USER_ID)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> activityService.deleteActivity(OTHER_USER_ID, ACTIVITY_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED);
                    });
        }
    }

    @Nested
    @DisplayName("getActivityDetail - 모임 상세 조회")
    class GetActivityDetail {

        private static final Long ACTIVITY_ID = 100L;

        @Test
        @DisplayName("성공: 개인 모임 상세 조회 (참가자 포함)")
        void successWithPersonalActivityAndParticipants() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Activity activity = createActivity(creator, null);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            User participant1 = User.builder()
                    .userId("user1")
                    .password("pwd")
                    .name("참가자1")
                    .phone("01011111111")
                    .profileImg("img1.jpg")
                    .build();
            ReflectionTestUtils.setField(participant1, "id", 2L);

            User participant2 = User.builder()
                    .userId("user2")
                    .password("pwd")
                    .name("참가자2")
                    .phone("01022222222")
                    .profileImg(null)
                    .build();
            ReflectionTestUtils.setField(participant2, "id", 3L);

            ActivityParticipant ap1 = ActivityParticipant.builder()
                    .activity(activity)
                    .participant(participant1)
                    .build();

            ActivityParticipant ap2 = ActivityParticipant.builder()
                    .activity(activity)
                    .participant(participant2)
                    .build();

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));
            given(activityParticipantRepository.findByActivityIdAndStatusWithParticipant(ACTIVITY_ID, BaseStatus.ACTIVE)).willReturn(List.of(ap1, ap2));

            // when
            GetActivityDetailResult result = activityService.getActivityDetail(ACTIVITY_ID);

            // then
            assertThat(result.activityId()).isEqualTo(ACTIVITY_ID);
            assertThat(result.name()).isEqualTo("테스트 모임");
            assertThat(result.description()).isEqualTo("테스트 모임입니다");
            assertThat(result.clubId()).isNull();
            assertThat(result.clubName()).isNull();
            assertThat(result.openType()).isEqualTo(OpenType.PUBLIC);
            assertThat(result.capacity()).isEqualTo(20);
            assertThat(result.participants()).hasSize(2);
            assertThat(result.participants().get(0).id()).isEqualTo(2L);
            assertThat(result.participants().get(0).name()).isEqualTo("참가자1");
            assertThat(result.participants().get(0).profileImg()).isEqualTo("img1.jpg");
            assertThat(result.participants().get(1).id()).isEqualTo(3L);
            assertThat(result.participants().get(1).name()).isEqualTo("참가자2");
            assertThat(result.participants().get(1).profileImg()).isNull();
        }

        @Test
        @DisplayName("성공: 동아리 모임 상세 조회")
        void successWithClubActivity() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Club club = createClub(creator);
            ReflectionTestUtils.setField(club, "id", CLUB_ID);

            Activity activity = createActivity(creator, club);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));
            given(activityParticipantRepository.findByActivityIdAndStatusWithParticipant(ACTIVITY_ID, BaseStatus.ACTIVE)).willReturn(Collections.emptyList());

            // when
            GetActivityDetailResult result = activityService.getActivityDetail(ACTIVITY_ID);

            // then
            assertThat(result.activityId()).isEqualTo(ACTIVITY_ID);
            assertThat(result.clubId()).isEqualTo(CLUB_ID);
            assertThat(result.clubName()).isEqualTo("테스트 동아리");
            assertThat(result.participants()).isEmpty();
        }

        @Test
        @DisplayName("성공: 참가자가 없는 모임 조회")
        void successWithNoParticipants() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Activity activity = createActivity(creator, null);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));
            given(activityParticipantRepository.findByActivityIdAndStatusWithParticipant(ACTIVITY_ID, BaseStatus.ACTIVE)).willReturn(Collections.emptyList());

            // when
            GetActivityDetailResult result = activityService.getActivityDetail(ACTIVITY_ID);

            // then
            assertThat(result.activityId()).isEqualTo(ACTIVITY_ID);
            assertThat(result.participants()).isEmpty();
        }

        @Test
        @DisplayName("성공: INACTIVE 참가자는 DB에서 필터링됨")
        void successFilteringInactiveParticipants() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Activity activity = createActivity(creator, null);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);

            User activeUser = User.builder()
                    .userId("active")
                    .password("pwd")
                    .name("활성 참가자")
                    .phone("01011111111")
                    .build();
            ReflectionTestUtils.setField(activeUser, "id", 2L);

            ActivityParticipant activeParticipant = ActivityParticipant.builder()
                    .activity(activity)
                    .participant(activeUser)
                    .build();

            // DB 레벨에서 ACTIVE만 반환 (INACTIVE는 쿼리에서 제외됨)
            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));
            given(activityParticipantRepository.findByActivityIdAndStatusWithParticipant(ACTIVITY_ID, BaseStatus.ACTIVE))
                    .willReturn(List.of(activeParticipant));

            // when
            GetActivityDetailResult result = activityService.getActivityDetail(ACTIVITY_ID);

            // then
            assertThat(result.participants()).hasSize(1);
            assertThat(result.participants().get(0).id()).isEqualTo(2L);
            assertThat(result.participants().get(0).name()).isEqualTo("활성 참가자");
        }

        @Test
        @DisplayName("실패: 모임 없음")
        void failWithActivityNotFound() {
            // given
            given(activityRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> activityService.getActivityDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 삭제된 모임 조회")
        void failWithInactiveActivity() {
            // given
            User creator = createUser();
            ReflectionTestUtils.setField(creator, "id", USER_ID);

            Activity activity = createActivity(creator, null);
            ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
            activity.inactivate();

            given(activityRepository.findById(ACTIVITY_ID)).willReturn(Optional.of(activity));

            // when & then
            assertThatThrownBy(() -> activityService.getActivityDetail(ACTIVITY_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ActivityErrorCode.ACTIVITY_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("getActivityList - 모임 목록 조회")
    class GetActivityList {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("성공: PERSONAL 섹션 조회")
        void successWithPersonalSection() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);

            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(1L, "thumb1.jpg", "PERSONAL", null, "개인 모임1",
                            "카테고리1", "서울", 5, 20, 10, 100, 3, false),
                    new ActivitySummaryResult(2L, null, "PERSONAL", null, "개인 모임2",
                            null, null, 0, null, 0, 0, 0, true)
            );
            Slice<ActivitySummaryResult> slice = new SliceImpl<>(content, pageable, false);

            given(activityRepository.findPersonalActivities(eq(USER_ID), any(Pageable.class))).willReturn(slice);

            GetActivityListParam param = new GetActivityListParam(ActivityListSection.PERSONAL, pageable);

            // when
            Slice<ActivitySummaryResult> result = activityService.getActivityList(USER_ID, param);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.getContent().get(0).name()).isEqualTo("개인 모임1");
            assertThat(result.getContent().get(0).activityType()).isEqualTo("PERSONAL");
            assertThat(result.getContent().get(1).isLiked()).isTrue();
            verify(activityRepository).findPersonalActivities(eq(USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("성공: LIKED_CLUBS 섹션 조회")
        void successWithLikedClubsSection() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);

            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(3L, "thumb3.jpg", "CLUB", "관심 동아리", "동아리 모임",
                            "운동", "부산", 10, 30, 5, 50, 1, true)
            );
            Slice<ActivitySummaryResult> slice = new SliceImpl<>(content, pageable, true);

            given(activityRepository.findActivitiesByLikedClubs(eq(USER_ID), any(Pageable.class))).willReturn(slice);

            GetActivityListParam param = new GetActivityListParam(ActivityListSection.LIKED_CLUBS, pageable);

            // when
            Slice<ActivitySummaryResult> result = activityService.getActivityList(USER_ID, param);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.getContent().get(0).clubName()).isEqualTo("관심 동아리");
            verify(activityRepository).findActivitiesByLikedClubs(eq(USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("성공: MY_CLUBS 섹션 조회")
        void successWithMyClubsSection() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);

            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(4L, null, "CLUB", "내 동아리", "정기 모임",
                            "스터디", "서울", 8, 15, 3, 20, 0, false)
            );
            Slice<ActivitySummaryResult> slice = new SliceImpl<>(content, pageable, false);

            given(activityRepository.findActivitiesByMyClubs(eq(USER_ID), any(Pageable.class))).willReturn(slice);

            GetActivityListParam param = new GetActivityListParam(ActivityListSection.MY_CLUBS, pageable);

            // when
            Slice<ActivitySummaryResult> result = activityService.getActivityList(USER_ID, param);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.getContent().get(0).clubName()).isEqualTo("내 동아리");
            verify(activityRepository).findActivitiesByMyClubs(eq(USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("성공: 빈 결과 반환")
        void successWithEmptyResult() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);

            Slice<ActivitySummaryResult> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);
            given(activityRepository.findPersonalActivities(eq(USER_ID), any(Pageable.class))).willReturn(emptySlice);

            GetActivityListParam param = new GetActivityListParam(ActivityListSection.PERSONAL, pageable);

            // when
            Slice<ActivitySummaryResult> result = activityService.getActivityList(USER_ID, param);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("성공: 페이지네이션 hasNext 검증")
        void successWithPagination() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);

            Pageable smallPage = PageRequest.of(0, 2);
            List<ActivitySummaryResult> content = List.of(
                    new ActivitySummaryResult(1L, null, "PERSONAL", null, "모임1",
                            null, null, 0, null, 0, 0, 0, false),
                    new ActivitySummaryResult(2L, null, "PERSONAL", null, "모임2",
                            null, null, 0, null, 0, 0, 0, false)
            );
            Slice<ActivitySummaryResult> slice = new SliceImpl<>(content, smallPage, true);

            given(activityRepository.findPersonalActivities(eq(USER_ID), any(Pageable.class))).willReturn(slice);

            GetActivityListParam param = new GetActivityListParam(ActivityListSection.PERSONAL, smallPage);

            // when
            Slice<ActivitySummaryResult> result = activityService.getActivityList(USER_ID, param);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failWithUserNotFound() {
            // given
            given(userRepository.existsById(999L)).willReturn(false);

            GetActivityListParam param = new GetActivityListParam(ActivityListSection.PERSONAL, pageable);

            // when & then
            assertThatThrownBy(() -> activityService.getActivityList(999L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    private User createUser() {
        return User.builder()
                .userId("dewple123")
                .password("encoded-password")
                .name("홍길동")
                .phone("01012345678")
                .build();
    }

    private Club createClub(User creator) {
        return Club.builder()
                .creator(creator)
                .name("테스트 동아리")
                .description("테스트용 동아리입니다")
                .gender(Gender.MALE)
                .activityType(ActivityType.BOTH)
                .build();
    }

    private Activity createActivity(User creator, Club club) {
        return Activity.builder()
                .creator(creator)
                .club(club)
                .openType(OpenType.PUBLIC)
                .name("테스트 모임")
                .description("테스트 모임입니다")
                .capacity(20)
                .isAttendanceCheck(false)
                .isSearchable(true)
                .startAt(START_AT)
                .endAt(END_AT)
                .build();
    }
}
