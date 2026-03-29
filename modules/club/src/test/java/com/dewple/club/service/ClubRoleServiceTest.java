package com.dewple.club.service;

import com.dewple.club.entity.ClubMember;
import com.dewple.club.entity.ClubRole;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.club.repository.ClubRepository;
import com.dewple.club.repository.ClubRoleRepository;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClubRoleServiceTest {

    @Mock private ClubRepository clubRepository;
    @Mock private ClubRoleRepository clubRoleRepository;
    @Mock private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private ClubRoleService clubRoleService;

    private static final Long USER_ID = 1L;
    private static final Long CLUB_ID = 100L;

    private User createUser(Long userId) {
        User user = User.builder().userId("user" + userId).name("유저" + userId).phone("010").build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Club createClub() {
        Club club = Club.builder()
                .creator(createUser(USER_ID)).name("코딩 동아리").activityType(ActivityType.BOTH)
                .isVerificationRequired(false).build();
        ReflectionTestUtils.setField(club, "id", CLUB_ID);
        return club;
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

    private void mockPresidentPermission(Club club) {
        ClubMember president = createPresidentMember(club);
        given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, USER_ID))
                .willReturn(Optional.of(president));
    }

    @Nested
    @DisplayName("createRole - 역할 생성")
    class CreateRole {

        @Test
        @DisplayName("성공: 회장이 커스텀 역할 생성")
        void success() {
            Club club = createClub();
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            mockPresidentPermission(club);
            given(clubRoleRepository.existsByClubIdAndName(CLUB_ID, "홍보담당")).willReturn(false);
            given(clubRoleRepository.save(any(ClubRole.class)))
                    .willAnswer(invocation -> {
                        ClubRole r = invocation.getArgument(0);
                        ReflectionTestUtils.setField(r, "id", 10L);
                        return r;
                    });

            CreateClubRoleParam param = new CreateClubRoleParam(
                    "홍보담당", List.of("MANAGE_NOTICE", "MANAGE_FEED"), true);

            ClubRoleResult result = clubRoleService.createRole(USER_ID, CLUB_ID, param);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("홍보담당");
            assertThat(result.permissions()).containsExactlyInAnyOrder("MANAGE_NOTICE", "MANAGE_FEED");
            assertThat(result.isStaff()).isTrue();
            assertThat(result.isDefault()).isFalse();
        }

        @Test
        @DisplayName("성공: 9번 권한(MANAGE_MEMBER) 보유자가 역할 생성")
        void successByMemberManager() {
            Club club = createClub();
            ClubRole managerRole = ClubRole.builder()
                    .club(club).name("인사팀장")
                    .permissions(Permission.combine(Permission.MANAGE_MEMBER))
                    .isStaff(true).isDefault(false).build();
            ReflectionTestUtils.setField(managerRole, "id", 5L);
            ClubMember manager = ClubMember.builder()
                    .club(club).user(createUser(2L)).role(managerRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(manager, "id", 51L);

            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, 2L))
                    .willReturn(Optional.of(manager));
            given(clubRoleRepository.existsByClubIdAndName(CLUB_ID, "신규역할")).willReturn(false);
            given(clubRoleRepository.save(any(ClubRole.class)))
                    .willAnswer(invocation -> {
                        ClubRole r = invocation.getArgument(0);
                        ReflectionTestUtils.setField(r, "id", 11L);
                        return r;
                    });

            CreateClubRoleParam param = new CreateClubRoleParam("신규역할", List.of(), false);

            ClubRoleResult result = clubRoleService.createRole(2L, CLUB_ID, param);

            assertThat(result.id()).isEqualTo(11L);
            assertThat(result.name()).isEqualTo("신규역할");
        }

        @Test
        @DisplayName("실패: 중복된 역할 이름")
        void failDuplicateName() {
            Club club = createClub();
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            mockPresidentPermission(club);
            given(clubRoleRepository.existsByClubIdAndName(CLUB_ID, "회장")).willReturn(true);

            CreateClubRoleParam param = new CreateClubRoleParam("회장", List.of(), false);

            assertThatThrownBy(() -> clubRoleService.createRole(USER_ID, CLUB_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.ROLE_NAME_DUPLICATED));
        }

        @Test
        @DisplayName("실패: 멤버가 아닌 유저")
        void failNotMember() {
            Club club = createClub();
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, 999L))
                    .willReturn(Optional.empty());

            CreateClubRoleParam param = new CreateClubRoleParam("역할", List.of(), false);

            assertThatThrownBy(() -> clubRoleService.createRole(999L, CLUB_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.NOT_CLUB_MEMBER));
        }

        @Test
        @DisplayName("실패: 권한 없는 멤버 (부원)")
        void failPermissionDenied() {
            Club club = createClub();
            ClubRole memberRole = ClubRole.builder()
                    .club(club).name("부원").permissions(0L)
                    .isStaff(false).isDefault(true).build();
            ReflectionTestUtils.setField(memberRole, "id", 5L);
            ClubMember member = ClubMember.builder()
                    .club(club).user(createUser(USER_ID)).role(memberRole)
                    .activityStatus(ActivityStatus.ACTIVE).build();
            ReflectionTestUtils.setField(member, "id", 51L);

            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubIdAndUserId(CLUB_ID, USER_ID))
                    .willReturn(Optional.of(member));

            CreateClubRoleParam param = new CreateClubRoleParam("역할", List.of(), false);

            assertThatThrownBy(() -> clubRoleService.createRole(USER_ID, CLUB_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.ROLE_MANAGE_FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("getRoles - 역할 목록 조회")
    class GetRoles {

        @Test
        @DisplayName("성공: 역할 목록 조회")
        void success() {
            Club club = createClub();
            ClubRole presidentRole = createPresidentRole(club);
            ClubRole customRole = ClubRole.builder()
                    .club(club).name("홍보담당")
                    .permissions(Permission.combine(Permission.MANAGE_NOTICE, Permission.MANAGE_FEED))
                    .isStaff(true).isDefault(false).build();
            ReflectionTestUtils.setField(customRole, "id", 10L);

            given(clubRepository.existsById(CLUB_ID)).willReturn(true);
            given(clubRoleRepository.findByClubId(CLUB_ID))
                    .willReturn(List.of(presidentRole, customRole));

            List<ClubRoleResult> results = clubRoleService.getRoles(CLUB_ID);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).name()).isEqualTo("회장");
            assertThat(results.get(0).isDefault()).isTrue();
            assertThat(results.get(1).name()).isEqualTo("홍보담당");
            assertThat(results.get(1).isDefault()).isFalse();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 동아리")
        void failNotFound() {
            given(clubRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> clubRoleService.getRoles(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND));
        }
    }
}
