package com.dewple.club.service;

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
            given(clubRepository.save(any(Club.class))).willAnswer(invocation -> {
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
}
