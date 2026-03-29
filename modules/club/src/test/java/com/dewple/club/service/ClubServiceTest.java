package com.dewple.club.service;

import com.dewple.club.entity.ClubDeletionVote;
import com.dewple.club.entity.ClubGeneration;
import com.dewple.club.entity.ClubKickVote;
import com.dewple.club.entity.ClubMember;
import com.dewple.club.entity.ClubRole;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.port.ClubRecruitmentPort;
import com.dewple.club.repository.*;
import com.dewple.common.entity.Category;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.Region;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ClubDeletionStatus;
import com.dewple.common.enums.Gender;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.common.exception.CommonErrorCode;
import com.dewple.common.repository.CategoryRepository;
import com.dewple.common.repository.RegionRepository;
import com.dewple.common.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock private ClubRepository clubRepository;
    @Mock private ClubRoleRepository clubRoleRepository;
    @Mock private ClubMemberRepository clubMemberRepository;
    @Mock private ClubCategoryRepository clubCategoryRepository;
    @Mock private ClubRegionRepository clubRegionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private ClubRecruitmentPort clubRecruitmentPort;
    @Mock private ClubDeletionVoteRepository clubDeletionVoteRepository;
    @Mock private ClubGenerationRepository clubGenerationRepository;
    @Mock private ClubKickVoteRepository clubKickVoteRepository;

    @InjectMocks
    private ClubService clubService;

    private static final Long USER_ID = 1L;

    private User createUser() {
        User user = User.builder().userId("testuser").name("테스트").phone("010").build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private Club createClub() {
        Club club = Club.builder()
                .creator(createUser()).name("코딩 동아리").activityType(ActivityType.BOTH)
                .isVerificationRequired(false).foundedDate(LocalDate.of(2024, 1, 1))
                .build();
        ReflectionTestUtils.setField(club, "id", 100L);
        return club;
    }

    private CreateClubParam createValidParam() {
        return new CreateClubParam(
                "코딩 동아리", false, ActivityType.BOTH,
                LocalDate.of(2024, 1, 1), List.of(1L, 2L), List.of(1L)
        );
    }

    @Nested
    @DisplayName("createClub - 동아리 생성")
    class CreateClub {

        @Test
        @DisplayName("성공: 유효한 정보로 동아리 생성")
        void success() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));
            given(clubMemberRepository.findByUserIdAndStatusAndActivityStatus(
                    USER_ID, BaseStatus.ACTIVE, ActivityStatus.ACTIVE)).willReturn(List.of());
            given(clubRepository.<Club>save(any(Club.class))).willAnswer(invocation -> {
                Club club = invocation.getArgument(0);
                ReflectionTestUtils.setField(club, "id", 100L);
                return club;
            });
            given(categoryRepository.findById(any())).willReturn(Optional.of(mock()));
            given(regionRepository.findById(any())).willReturn(Optional.of(mock()));

            ClubRole presidentRole = ClubRole.builder().name("회장")
                    .permissions(Permission.all()).isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(presidentRole, "id", 1L);
            given(clubRoleRepository.findByClubIdAndName(100L, "회장"))
                    .willReturn(Optional.of(presidentRole));

            // when
            CreateClubResult result = clubService.createClub(USER_ID, createValidParam());

            // then
            assertThat(result.clubId()).isEqualTo(100L);
            assertThat(result.name()).isEqualTo("코딩 동아리");
            assertThat(result.activityType()).isEqualTo(ActivityType.BOTH);
            assertThat(result.isVerificationRequired()).isFalse();
            assertThat(result.creatorId()).isEqualTo(USER_ID);
            then(clubRoleRepository).should().saveAll(any());
            then(clubMemberRepository).should().save(any(ClubMember.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저")
        void failUserNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.createClub(999L, createValidParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(CommonErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("실패: 카테고리 미선택")
        void failNoCategoryIds() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateClubParam param = new CreateClubParam(
                    "테스트", false, ActivityType.BOTH, null, List.of(), List.of(1L));

            assertThatThrownBy(() -> clubService.createClub(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_CATEGORY_REQUIRED));
        }

        @Test
        @DisplayName("실패: 카테고리 4개 초과")
        void failTooManyCategories() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateClubParam param = new CreateClubParam(
                    "테스트", false, ActivityType.BOTH, null,
                    List.of(1L, 2L, 3L, 4L), List.of(1L));

            assertThatThrownBy(() -> clubService.createClub(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_CATEGORY_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("실패: 지역 미선택")
        void failNoRegionIds() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateClubParam param = new CreateClubParam(
                    "테스트", false, ActivityType.BOTH, null, List.of(1L), List.of());

            assertThatThrownBy(() -> clubService.createClub(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_REGION_REQUIRED));
        }

        @Test
        @DisplayName("실패: 회장 동시 운영 5개 초과")
        void failPresidentLimitExceeded() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            ClubRole presidentRole = ClubRole.builder().name("회장")
                    .permissions(Permission.all()).isStaff(true).isDefault(true).build();

            List<ClubMember> existingMemberships = List.of(
                    ClubMember.builder().role(presidentRole).activityStatus(ActivityStatus.ACTIVE).build(),
                    ClubMember.builder().role(presidentRole).activityStatus(ActivityStatus.ACTIVE).build(),
                    ClubMember.builder().role(presidentRole).activityStatus(ActivityStatus.ACTIVE).build(),
                    ClubMember.builder().role(presidentRole).activityStatus(ActivityStatus.ACTIVE).build(),
                    ClubMember.builder().role(presidentRole).activityStatus(ActivityStatus.ACTIVE).build()
            );

            given(clubMemberRepository.findByUserIdAndStatusAndActivityStatus(
                    USER_ID, BaseStatus.ACTIVE, ActivityStatus.ACTIVE)).willReturn(existingMemberships);

            assertThatThrownBy(() -> clubService.createClub(USER_ID, createValidParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_PRESIDENT_LIMIT_EXCEEDED));
        }
    }

    @Nested
    @DisplayName("getClubDetail - 동아리 단건 조회")
    class GetClubDetail {

        @Test
        @DisplayName("성공: 동아리 상세 조회")
        void success() {
            Club club = createClub();
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));

            ClubDetailResult result = clubService.getClubDetail(100L);

            assertThat(result.id()).isEqualTo(100L);
            assertThat(result.name()).isEqualTo("코딩 동아리");
            assertThat(result.activityType()).isEqualTo(ActivityType.BOTH);
            assertThat(result.creatorId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 동아리")
        void failNotFound() {
            given(clubRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.getClubDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getClubList - 동아리 목록 조회")
    class GetClubList {

        @Test
        @DisplayName("성공: 필터 없이 목록 조회")
        void success() {
            List<ClubSummaryResult> content = List.of(
                    new ClubSummaryResult(1L, "코딩 동아리", null, ActivityType.BOTH, false, 10, 50L),
                    new ClubSummaryResult(2L, "등산 동아리", "cover.jpg", ActivityType.OFFLINE, false, 5, 30L)
            );
            Slice<ClubSummaryResult> slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);
            given(clubRepository.findClubList(any(GetClubListParam.class))).willReturn(slice);

            GetClubListParam param = new GetClubListParam(null, null, null, null, null, PageRequest.of(0, 10));
            Slice<ClubSummaryResult> result = clubService.getClubList(param);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).name()).isEqualTo("코딩 동아리");
            assertThat(result.getContent().get(0).memberCount()).isEqualTo(50L);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("성공: 빈 결과")
        void successEmpty() {
            Slice<ClubSummaryResult> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);
            given(clubRepository.findClubList(any(GetClubListParam.class))).willReturn(slice);

            GetClubListParam param = new GetClubListParam(null, null, 999L, null, null, PageRequest.of(0, 10));
            Slice<ClubSummaryResult> result = clubService.getClubList(param);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("inviteGuest - 외부인 GUEST 초대")
    class InviteGuest {

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        private ClubMember createPresidentMember(Club club) {
            ClubRole role = createPresidentRole(club);
            ClubMember member = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(role)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 50L);
            return member;
        }

        @Test
        @DisplayName("성공: GUEST 초대")
        void success() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);
            User guestUser = User.builder().userId("guest").name("게스트").phone("010").build();
            ReflectionTestUtils.setField(guestUser, "id", 5L);
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndUserId(100L, 5L))
                    .willReturn(Optional.empty());
            given(userRepository.findById(5L)).willReturn(Optional.of(guestUser));
            given(clubRoleRepository.findByClubIdAndName(100L, "부원"))
                    .willReturn(Optional.of(memberRole));
            given(clubMemberRepository.save(any(ClubMember.class)))
                    .willAnswer(invocation -> {
                        ClubMember m = invocation.getArgument(0);
                        ReflectionTestUtils.setField(m, "id", 60L);
                        return m;
                    });

            InviteGuestParam param = new InviteGuestParam(5L, List.of(1L));
            ClubMemberResult result = clubService.inviteGuest(USER_ID, 100L, param);

            assertThat(result.activityStatus()).isEqualTo(ActivityStatus.GUEST);
            assertThat(result.roleName()).isEqualTo("부원");
        }

        @Test
        @DisplayName("실패: 이미 멤버인 유저 초대")
        void failAlreadyMember() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndUserId(100L, 5L))
                    .willReturn(Optional.of(mock(ClubMember.class)));

            InviteGuestParam param = new InviteGuestParam(5L, List.of(1L));

            assertThatThrownBy(() -> clubService.inviteGuest(USER_ID, 100L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.ALREADY_CLUB_MEMBER));
        }

        @Test
        @DisplayName("실패: 모임 미지정")
        void failNoActivity() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));

            InviteGuestParam param = new InviteGuestParam(5L, List.of());

            assertThatThrownBy(() -> clubService.inviteGuest(USER_ID, 100L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.ACTIVITY_ID_REQUIRED));
        }
    }

    @Nested
    @DisplayName("promoteToMember - GUEST→MEMBER 승격")
    class PromoteToMember {

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        private ClubMember createPresidentMember(Club club) {
            ClubRole role = createPresidentRole(club);
            ClubMember member = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(role)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 50L);
            return member;
        }

        @Test
        @DisplayName("성공: GUEST를 MEMBER로 승격")
        void success() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);

            User guestUser = User.builder().userId("guest").name("게스트").phone("010").build();
            ReflectionTestUtils.setField(guestUser, "id", 5L);
            ClubMember guest = ClubMember.builder()
                    .club(club).user(guestUser).role(memberRole)
                    .activityStatus(ActivityStatus.GUEST).build();
            ReflectionTestUtils.setField(guest, "id", 60L);

            ClubGeneration generation = ClubGeneration.builder()
                    .club(club).generationNo(1)
                    .startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 12, 31))
                    .build();
            ReflectionTestUtils.setField(generation, "id", 10L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndId(100L, 60L))
                    .willReturn(Optional.of(guest));
            given(clubGenerationRepository.findById(10L)).willReturn(Optional.of(generation));

            PromoteToMemberParam param = new PromoteToMemberParam(10L, LocalDate.of(2026, 12, 31));
            clubService.promoteToMember(USER_ID, 100L, 60L, param);

            assertThat(guest.getActivityStatus()).isEqualTo(ActivityStatus.ACTIVE);
            assertThat(guest.getJoinGeneration()).isEqualTo(generation);
            assertThat(guest.getActivityEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("실패: GUEST가 아닌 멤버 승격 시도")
        void failNotGuest() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);
            ClubMember activeMember = ClubMember.builder()
                    .club(club).user(createUser()).role(memberRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(activeMember, "id", 60L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndId(100L, 60L))
                    .willReturn(Optional.of(activeMember));

            PromoteToMemberParam param = new PromoteToMemberParam(10L, LocalDate.of(2026, 12, 31));

            assertThatThrownBy(() -> clubService.promoteToMember(USER_ID, 100L, 60L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.NOT_GUEST_STATUS));
        }
    }

    @Nested
    @DisplayName("requestKick - 회원 내보내기 신청")
    class RequestKick {

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        private ClubMember createPresidentMember(Club club) {
            ClubRole role = createPresidentRole(club);
            ClubMember member = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(role)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 50L);
            return member;
        }

        @Test
        @DisplayName("성공: 권한자 1명 → 즉시 내보내기")
        void successSingleVoter() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);
            User targetUser = User.builder().userId("target").name("대상").phone("010").build();
            ReflectionTestUtils.setField(targetUser, "id", 2L);
            ClubMember target = ClubMember.builder()
                    .club(club).user(targetUser).role(memberRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(target, "id", 60L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndId(100L, 60L))
                    .willReturn(Optional.of(target));
            given(clubKickVoteRepository.existsByTargetMember(target)).willReturn(false);
            given(clubMemberRepository.findByClubIdAndStatusAndActivityStatus(
                    100L, BaseStatus.ACTIVE, ActivityStatus.ACTIVE))
                    .willReturn(List.of(president));

            clubService.requestKick(USER_ID, 100L, 60L);

            assertThat(target.getActivityStatus()).isEqualTo(ActivityStatus.KICKEDOUT);
        }

        @Test
        @DisplayName("실패: 이미 내보내기 투표 진행 중")
        void failAlreadyInProgress() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);
            ClubMember target = mock(ClubMember.class);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndId(100L, 60L))
                    .willReturn(Optional.of(target));
            given(clubKickVoteRepository.existsByTargetMember(target)).willReturn(true);

            assertThatThrownBy(() -> clubService.requestKick(USER_ID, 100L, 60L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.KICK_ALREADY_IN_PROGRESS));
        }
    }

    @Nested
    @DisplayName("voteKick - 회원 내보내기 투표")
    class VoteKick {

        @Test
        @DisplayName("성공: 동의 → 전원 동의 → 내보내기 완료")
        void successAllApproved() {
            Club club = createClub();
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);
            User targetUser = User.builder().userId("target").name("대상").phone("010").build();
            ReflectionTestUtils.setField(targetUser, "id", 2L);
            ClubMember target = ClubMember.builder()
                    .club(club).user(targetUser).role(memberRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(target, "id", 60L);

            ClubKickVote vote = ClubKickVote.builder()
                    .club(club).targetMember(target).voter(createUser()).isApproved(null).build();
            ReflectionTestUtils.setField(vote, "id", 1L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndId(100L, 60L)).willReturn(Optional.of(target));
            given(clubKickVoteRepository.existsByTargetMember(target)).willReturn(true);
            given(clubKickVoteRepository.findByTargetMemberAndVoterId(target, USER_ID))
                    .willReturn(Optional.of(vote));
            given(clubKickVoteRepository.findByTargetMember(target)).willReturn(List.of(vote));

            clubService.voteKick(USER_ID, 100L, 60L, true);

            assertThat(vote.getIsApproved()).isTrue();
            assertThat(target.getActivityStatus()).isEqualTo(ActivityStatus.KICKEDOUT);
        }

        @Test
        @DisplayName("성공: 거부 → 투표 종료")
        void successRejected() {
            Club club = createClub();
            ClubMember target = mock(ClubMember.class);
            ClubKickVote vote = ClubKickVote.builder()
                    .club(club).targetMember(target).voter(createUser()).isApproved(null).build();

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndId(100L, 60L)).willReturn(Optional.of(target));
            given(clubKickVoteRepository.existsByTargetMember(target)).willReturn(true);
            given(clubKickVoteRepository.findByTargetMemberAndVoterId(target, USER_ID))
                    .willReturn(Optional.of(vote));

            clubService.voteKick(USER_ID, 100L, 60L, false);

            then(clubKickVoteRepository).should().deleteByTargetMember(target);
        }
    }

    @Nested
    @DisplayName("processGraduation - 자동 수료 처리")
    class ProcessGraduation {

        @Test
        @DisplayName("성공: 활동 종료일 지난 ACTIVE 멤버 수료 처리")
        void success() {
            Club club = createClub();
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);

            ClubMember expired = ClubMember.builder()
                    .club(club).user(createUser()).role(memberRole)
                    .activityStatus(ActivityStatus.ACTIVE)
                    .activityEndDate(LocalDate.now().minusDays(1))
                    .build();
            ReflectionTestUtils.setField(expired, "id", 60L);

            given(clubMemberRepository.findByActivityStatusAndActivityEndDateLessThanEqual(
                    eq(ActivityStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(expired));

            int count = clubService.processGraduation();

            assertThat(count).isEqualTo(1);
            assertThat(expired.getActivityStatus()).isEqualTo(ActivityStatus.GRADUATED);
        }

        @Test
        @DisplayName("성공: 수료 대상 없음")
        void successNoTargets() {
            given(clubMemberRepository.findByActivityStatusAndActivityEndDateLessThanEqual(
                    eq(ActivityStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of());

            int count = clubService.processGraduation();

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getMembers - 동아리 회원 목록 조회")
    class GetMembers {

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        @Test
        @DisplayName("성공: 전체 멤버 조회")
        void successAll() {
            Club club = createClub();
            ClubRole presidentRole = createPresidentRole(club);
            ClubMember president = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(presidentRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(president, "id", 50L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubId(100L)).willReturn(List.of(president));

            List<ClubMemberResult> results = clubService.getMembers(USER_ID, 100L, null);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).roleName()).isEqualTo("회장");
        }

        @Test
        @DisplayName("성공: 상태별 필터 조회")
        void successFiltered() {
            Club club = createClub();
            ClubRole presidentRole = createPresidentRole(club);
            ClubMember president = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(presidentRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(president, "id", 50L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndActivityStatus(100L, ActivityStatus.GUEST))
                    .willReturn(List.of());

            List<ClubMemberResult> results = clubService.getMembers(USER_ID, 100L, ActivityStatus.GUEST);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("실패: 멤버가 아닌 유저")
        void failNotMember() {
            Club club = createClub();
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.getMembers(999L, 100L, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.NOT_CLUB_MEMBER));
        }
    }

    @Nested
    @DisplayName("updateClub - 동아리 정보 수정")
    class UpdateClub {

        private UpdateClubParam createUpdateParam() {
            return new UpdateClubParam(
                    "수정된 동아리", "수정된 설명", "new-cover.jpg",
                    ActivityType.ONLINE, LocalDate.of(2023, 6, 1),
                    List.of(2L, 3L), List.of(2L)
            );
        }

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        private ClubMember createPresidentMember(Club club) {
            ClubRole role = createPresidentRole(club);
            ClubMember member = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(role)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 50L);
            return member;
        }

        @Test
        @DisplayName("성공: 3번 권한 보유자의 정보 수정")
        void success() {
            Club club = createClub();
            ClubMember presidentMember = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(presidentMember));
            given(categoryRepository.findById(any())).willReturn(Optional.of(mock(Category.class)));
            given(regionRepository.findById(any())).willReturn(Optional.of(mock(Region.class)));

            clubService.updateClub(USER_ID, 100L, createUpdateParam());

            assertThat(club.getName()).isEqualTo("수정된 동아리");
            assertThat(club.getDescription()).isEqualTo("수정된 설명");
            assertThat(club.getCoverImg()).isEqualTo("new-cover.jpg");
            assertThat(club.getActivityType()).isEqualTo(ActivityType.ONLINE);
            assertThat(club.getFoundedDate()).isEqualTo(LocalDate.of(2023, 6, 1));
            then(clubCategoryRepository).should().deleteByClubId(100L);
            then(clubRegionRepository).should().deleteByClubId(100L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 동아리")
        void failNotFound() {
            given(clubRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.updateClub(USER_ID, 999L, createUpdateParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND));
        }

        @Test
        @DisplayName("실패: 멤버가 아닌 유저")
        void failNotMember() {
            Club club = createClub();
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.updateClub(999L, 100L, createUpdateParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.NOT_CLUB_MEMBER));
        }

        @Test
        @DisplayName("실패: 권한 없는 멤버")
        void failPermissionDenied() {
            Club club = createClub();
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);
            ClubMember member = ClubMember.builder()
                    .club(club).user(createUser()).role(memberRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 51L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(member));

            assertThatThrownBy(() -> clubService.updateClub(USER_ID, 100L, createUpdateParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_PERMISSION_DENIED));
        }

        @Test
        @DisplayName("실패: 카테고리 미선택")
        void failNoCategoryIds() {
            Club club = createClub();
            ClubMember presidentMember = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(presidentMember));

            UpdateClubParam param = new UpdateClubParam(
                    "이름", null, null, ActivityType.BOTH, null, List.of(), List.of(1L));

            assertThatThrownBy(() -> clubService.updateClub(USER_ID, 100L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_CATEGORY_REQUIRED));
        }

        @Test
        @DisplayName("실패: 카테고리 4개 초과")
        void failTooManyCategories() {
            Club club = createClub();
            ClubMember presidentMember = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(presidentMember));

            UpdateClubParam param = new UpdateClubParam(
                    "이름", null, null, ActivityType.BOTH, null,
                    List.of(1L, 2L, 3L, 4L), List.of(1L));

            assertThatThrownBy(() -> clubService.updateClub(USER_ID, 100L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_CATEGORY_LIMIT_EXCEEDED));
        }
    }

    @Nested
    @DisplayName("updateClubSettings - 동아리 설정 변경")
    class UpdateClubSettings {

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        private ClubMember createPresidentMember(Club club) {
            ClubRole role = createPresidentRole(club);
            ClubMember member = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(role)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 50L);
            return member;
        }

        @Test
        @DisplayName("성공: 본인인증 필수 활성화 + 성별/연령대 설정")
        void successEnableVerification() {
            Club club = createClub();
            ClubMember presidentMember = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(presidentMember));
            given(clubRecruitmentPort.hasActiveRecruitment(100L)).willReturn(false);

            UpdateClubSettingsParam param = new UpdateClubSettingsParam(true, Gender.MALE, 20L, 30L);

            clubService.updateClubSettings(USER_ID, 100L, param);

            assertThat(club.getIsVerificationRequired()).isTrue();
            assertThat(club.getGender()).isEqualTo(Gender.MALE);
            assertThat(club.getMinAge()).isEqualTo(20L);
            assertThat(club.getMaxAge()).isEqualTo(30L);
        }

        @Test
        @DisplayName("성공: 본인인증 비활성화 → 연령대/성별 자동 해제")
        void successDisableVerificationClearsGenderAge() {
            Club club = createClub();
            ClubMember presidentMember = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(presidentMember));
            given(clubRecruitmentPort.hasActiveRecruitment(100L)).willReturn(false);

            UpdateClubSettingsParam param = new UpdateClubSettingsParam(false, null, null, null);

            clubService.updateClubSettings(USER_ID, 100L, param);

            assertThat(club.getIsVerificationRequired()).isFalse();
            assertThat(club.getGender()).isEqualTo(Gender.ANY);
            assertThat(club.getMinAge()).isNull();
            assertThat(club.getMaxAge()).isNull();
        }

        @Test
        @DisplayName("실패: 활성 모집 공고가 있을 때 변경 시도")
        void failActiveRecruitmentExists() {
            Club club = createClub();
            ClubMember presidentMember = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(presidentMember));
            given(clubRecruitmentPort.hasActiveRecruitment(100L)).willReturn(true);

            UpdateClubSettingsParam param = new UpdateClubSettingsParam(true, Gender.ANY, null, null);

            assertThatThrownBy(() -> clubService.updateClubSettings(USER_ID, 100L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.ACTIVE_RECRUITMENT_EXISTS));
        }

        @Test
        @DisplayName("실패: 본인인증 없이 성별 설정 시도")
        void failGenderWithoutVerification() {
            Club club = createClub();
            ClubMember presidentMember = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(presidentMember));
            given(clubRecruitmentPort.hasActiveRecruitment(100L)).willReturn(false);

            UpdateClubSettingsParam param = new UpdateClubSettingsParam(false, Gender.MALE, null, null);

            assertThatThrownBy(() -> clubService.updateClubSettings(USER_ID, 100L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.VERIFICATION_REQUIRED_FOR_TAG));
        }

        @Test
        @DisplayName("실패: 권한 없는 멤버")
        void failPermissionDenied() {
            Club club = createClub();
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);
            ClubMember member = ClubMember.builder()
                    .club(club).user(createUser()).role(memberRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 51L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(member));

            UpdateClubSettingsParam param = new UpdateClubSettingsParam(true, Gender.ANY, null, null);

            assertThatThrownBy(() -> clubService.updateClubSettings(USER_ID, 100L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_PERMISSION_DENIED));
        }
    }

    @Nested
    @DisplayName("requestDeletion - 동아리 삭제 신청")
    class RequestDeletion {

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        private ClubMember createPresidentMember(Club club) {
            ClubRole role = createPresidentRole(club);
            ClubMember member = ClubMember.builder()
                    .club(club).user(club.getCreator()).role(role)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 50L);
            return member;
        }

        @Test
        @DisplayName("성공: 권한자 1명 → 즉시 승인")
        void successSingleVoter() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndStatusAndActivityStatus(
                    100L, BaseStatus.ACTIVE, ActivityStatus.ACTIVE))
                    .willReturn(List.of(president));

            clubService.requestDeletion(USER_ID, 100L);

            assertThat(club.getDeletionStatus()).isEqualTo(ClubDeletionStatus.APPROVED);
            assertThat(club.getScheduledDeleteAt()).isNotNull();
        }

        @Test
        @DisplayName("성공: 권한자 여러 명 → 투표 시작")
        void successMultipleVoters() {
            Club club = createClub();
            ClubMember president = createPresidentMember(club);

            User otherUser = User.builder().userId("other").name("기타").phone("010").build();
            ReflectionTestUtils.setField(otherUser, "id", 2L);
            ClubRole viceRole = ClubRole.builder()
                    .club(club).name("부회장")
                    .permissions(Permission.combine(Permission.DELETE_CLUB))
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(viceRole, "id", 2L);
            ClubMember vice = ClubMember.builder()
                    .club(club).user(otherUser).role(viceRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(vice, "id", 51L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));
            given(clubMemberRepository.findByClubIdAndStatusAndActivityStatus(
                    100L, BaseStatus.ACTIVE, ActivityStatus.ACTIVE))
                    .willReturn(List.of(president, vice));

            clubService.requestDeletion(USER_ID, 100L);

            assertThat(club.getDeletionStatus()).isEqualTo(ClubDeletionStatus.VOTING);
        }

        @Test
        @DisplayName("실패: 이미 삭제 진행 중")
        void failAlreadyInProgress() {
            Club club = createClub();
            club.startDeletionVoting();
            ClubMember president = createPresidentMember(club);
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));

            assertThatThrownBy(() -> clubService.requestDeletion(USER_ID, 100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.DELETION_ALREADY_IN_PROGRESS));
        }
    }

    @Nested
    @DisplayName("voteDeletion - 동아리 삭제 투표")
    class VoteDeletion {

        @Test
        @DisplayName("성공: 동의 → 전원 동의 → 승인")
        void successAllApproved() {
            Club club = createClub();
            club.startDeletionVoting();
            ClubDeletionVote vote = ClubDeletionVote.builder()
                    .club(club).user(createUser()).isApproved(null).build();
            ReflectionTestUtils.setField(vote, "id", 1L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubDeletionVoteRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(vote));
            given(clubDeletionVoteRepository.findByClubId(100L)).willReturn(List.of(vote));

            clubService.voteDeletion(USER_ID, 100L, true);

            assertThat(vote.getIsApproved()).isTrue();
            assertThat(club.getDeletionStatus()).isEqualTo(ClubDeletionStatus.APPROVED);
        }

        @Test
        @DisplayName("성공: 거부 → 투표 종료")
        void successRejected() {
            Club club = createClub();
            club.startDeletionVoting();
            ClubDeletionVote vote = ClubDeletionVote.builder()
                    .club(club).user(createUser()).isApproved(null).build();

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubDeletionVoteRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(vote));

            clubService.voteDeletion(USER_ID, 100L, false);

            assertThat(club.getDeletionStatus()).isEqualTo(ClubDeletionStatus.NONE);
        }

        @Test
        @DisplayName("실패: 투표 진행 중이 아님")
        void failNotVoting() {
            Club club = createClub();
            given(clubRepository.findById(100L)).willReturn(Optional.of(club));

            assertThatThrownBy(() -> clubService.voteDeletion(USER_ID, 100L, true))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.DELETION_NOT_VOTING));
        }
    }

    @Nested
    @DisplayName("cancelDeletion - 동아리 삭제 취소")
    class CancelDeletion {

        private ClubRole createPresidentRole(Club club) {
            ClubRole role = ClubRole.builder()
                    .club(club).name("회장").permissions(Permission.all())
                    .isStaff(true).isDefault(true).build();
            ReflectionTestUtils.setField(role, "id", 1L);
            return role;
        }

        @Test
        @DisplayName("성공: 유예 기간 중 회장이 취소")
        void success() {
            Club club = createClub();
            club.startDeletionVoting();
            club.approveDeletion();
            ClubRole presidentRole = createPresidentRole(club);
            ClubMember president = ClubMember.builder()
                    .club(club).user(createUser()).role(presidentRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(president, "id", 50L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));

            clubService.cancelDeletion(USER_ID, 100L);

            assertThat(club.getDeletionStatus()).isEqualTo(ClubDeletionStatus.NONE);
            assertThat(club.getScheduledDeleteAt()).isNull();
        }

        @Test
        @DisplayName("실패: 제재 삭제 취소 불가")
        void failSanction() {
            Club club = createClub();
            club.startDeletionVoting();
            club.approveDeletion();
            ReflectionTestUtils.setField(club, "isSanctionDeletion", true);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));

            assertThatThrownBy(() -> clubService.cancelDeletion(USER_ID, 100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.DELETION_SANCTION_NOT_CANCELABLE));
        }

        @Test
        @DisplayName("실패: 유예 기간 만료")
        void failExpired() {
            Club club = createClub();
            club.startDeletionVoting();
            club.approveDeletion();
            ReflectionTestUtils.setField(club, "scheduledDeleteAt", LocalDateTime.now().minusHours(1));
            ClubRole presidentRole = createPresidentRole(club);
            ClubMember president = ClubMember.builder()
                    .club(club).user(createUser()).role(presidentRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(president, "id", 50L);

            given(clubRepository.findById(100L)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(president));

            assertThatThrownBy(() -> clubService.cancelDeletion(USER_ID, 100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.DELETION_GRACE_PERIOD_EXPIRED));
        }
    }
}
