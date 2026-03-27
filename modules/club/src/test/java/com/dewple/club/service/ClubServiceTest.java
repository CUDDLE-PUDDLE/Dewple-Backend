package com.dewple.club.service;

import com.dewple.club.entity.ClubMember;
import com.dewple.club.entity.ClubRole;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.*;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock private ClubRepository clubRepository;
    @Mock private ClubRoleRepository clubRoleRepository;
    @Mock private ClubMemberRepository clubMemberRepository;
    @Mock private ClubCategoryRepository clubCategoryRepository;
    @Mock private ClubRegionRepository clubRegionRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private ClubService clubService;

    private static final Long USER_ID = 1L;

    private User createUser() {
        User user = User.builder().userId("testuser").name("테스트").phone("010").build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
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
            given(entityManager.getReference(any(), any())).willReturn(null);

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
                            .isEqualTo(UserErrorCode.USER_NOT_FOUND));
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
}
