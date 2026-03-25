package com.dewple.organization.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.*;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.entity.OrganizationMember;
import com.dewple.organization.entity.OrganizationRole;
import com.dewple.organization.exception.OrganizationErrorCode;
import com.dewple.organization.repository.OrganizationMemberRepository;
import com.dewple.organization.repository.OrganizationRepository;
import com.dewple.organization.repository.OrganizationRoleRepository;
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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRoleService organizationRoleService;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private OrganizationRoleRepository organizationRoleRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private static final Long USER_ID = 1L;
    private static final Long ORG_ID = 100L;

    private User createUser() {
        User user = User.builder()
                .userId("testuser")
                .name("테스트")
                .phone("01012345678")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private CreateOrganizationParam createValidParam() {
        return new CreateOrganizationParam(
                "서울대 동아리 연합회",
                "서울대학교 동아리들을 관리합니다.",
                OrganizationType.UNIVERSITY,
                ActivityType.BOTH,
                "contact@snu.ac.kr",
                "01012345678",
                ContactPreference.EMAIL,
                null,
                List.of(10L, 20L),
                List.of(1L, 2L),
                List.of(1L)
        );
    }

    private Organization createApprovedOrganization() {
        Organization org = createPendingOrganization();
        org.approve();
        return org;
    }

    private Organization createPendingOrganization() {
        Organization org = Organization.builder()
                .creator(createUser())
                .name("테스트 연합회")
                .purpose("목적")
                .type(OrganizationType.UNIVERSITY)
                .activityType(ActivityType.BOTH)
                .contactEmail("a@b.com")
                .contactPhone("010")
                .contactPreference(ContactPreference.EMAIL)
                .categoryIds("[1]")
                .regionIds("[1]")
                .build();
        ReflectionTestUtils.setField(org, "id", ORG_ID);
        return org;
    }

    private OrganizationRole createRepresentativeRole(Organization org) {
        OrganizationRole role = OrganizationRole.builder()
                .organization(org)
                .name("대표")
                .permissions(OrganizationPermission.all())
                .isStaff(true)
                .isDefault(true)
                .build();
        ReflectionTestUtils.setField(role, "id", 1L);
        return role;
    }

    private OrganizationMember createRepMember(Organization org) {
        OrganizationRole repRole = createRepresentativeRole(org);
        OrganizationMember member = OrganizationMember.builder()
                .organization(org).user(org.getCreator()).role(repRole).build();
        ReflectionTestUtils.setField(member, "id", 50L);
        return member;
    }

    private void mockPermission(Long orgId, Long userId, OrganizationMember member) {
        given(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId))
                .willReturn(Optional.of(member));
    }

    @Nested
    @DisplayName("apply - 연합회 생성 신청")
    class Apply {

        @Test
        @DisplayName("성공: 유효한 정보로 신청")
        void success() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));
            given(organizationRepository.save(any(Organization.class)))
                    .willAnswer(invocation -> {
                        Organization org = invocation.getArgument(0);
                        ReflectionTestUtils.setField(org, "id", ORG_ID);
                        return org;
                    });

            Organization result = organizationService.apply(USER_ID, createValidParam());

            assertThat(result.getId()).isEqualTo(ORG_ID);
            assertThat(result.getName()).isEqualTo("서울대 동아리 연합회");
            assertThat(result.getType()).isEqualTo(OrganizationType.UNIVERSITY);
            assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
            assertThat(result.getTargetClubIds()).isEqualTo("[10, 20]");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저")
        void failUserNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.apply(999L, createValidParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("실패: 카테고리 미선택")
        void failNoCategoryIds() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateOrganizationParam param = new CreateOrganizationParam(
                    "테스트", "목적", OrganizationType.OTHER, ActivityType.BOTH,
                    "a@b.com", "010", ContactPreference.EMAIL, null, null,
                    List.of(), List.of(1L));

            assertThatThrownBy(() -> organizationService.apply(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.CATEGORY_REQUIRED));
        }

        @Test
        @DisplayName("실패: 카테고리 4개 초과")
        void failTooManyCategories() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateOrganizationParam param = new CreateOrganizationParam(
                    "테스트", "목적", OrganizationType.OTHER, ActivityType.BOTH,
                    "a@b.com", "010", ContactPreference.EMAIL, null, null,
                    List.of(1L, 2L, 3L, 4L), List.of(1L));

            assertThatThrownBy(() -> organizationService.apply(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.CATEGORY_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("실패: 지역 미선택")
        void failNoRegionIds() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(createUser()));

            CreateOrganizationParam param = new CreateOrganizationParam(
                    "테스트", "목적", OrganizationType.OTHER, ActivityType.BOTH,
                    "a@b.com", "010", ContactPreference.EMAIL, null, null,
                    List.of(1L), List.of());

            assertThatThrownBy(() -> organizationService.apply(USER_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.REGION_REQUIRED));
        }
    }

    @Nested
    @DisplayName("approve - 연합회 생성 승인")
    class Approve {

        @Test
        @DisplayName("성공: 승인 후 기본 역할 초기화 및 대표 멤버 등록")
        void success() {
            Organization org = createPendingOrganization();
            OrganizationRole repRole = createRepresentativeRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findByOrganizationIdAndName(ORG_ID, "대표"))
                    .willReturn(Optional.of(repRole));
            given(organizationMemberRepository.save(any(OrganizationMember.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            organizationService.approve(ORG_ID);

            assertThat(org.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
            then(organizationRoleService).should().initializeDefaultRoles(org);
            then(organizationMemberRepository).should().save(any(OrganizationMember.class));
        }

        @Test
        @DisplayName("실패: 이미 승인된 연합회")
        void failAlreadyApproved() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.approve(ORG_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_PENDING));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 연합회")
        void failNotFound() {
            given(organizationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.approve(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("reject - 연합회 생성 반려")
    class Reject {

        @Test
        @DisplayName("성공: 반려 사유와 함께 반려")
        void success() {
            Organization org = createPendingOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            organizationService.reject(ORG_ID, "관리 대상 동아리 정보가 불충분합니다.");

            assertThat(org.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(org.getRejectionReason()).isEqualTo("관리 대상 동아리 정보가 불충분합니다.");
        }

        @Test
        @DisplayName("실패: 이미 반려된 연합회")
        void failAlreadyRejected() {
            Organization org = createPendingOrganization();
            org.reject("이전 사유");
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.reject(ORG_ID, "다른 사유"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_PENDING));
        }
    }

    @Nested
    @DisplayName("updateOrganization - 연합회 정보 수정")
    class UpdateOrganization {

        private UpdateOrganizationParam createUpdateParam() {
            return new UpdateOrganizationParam(
                    "수정된 연합회", "수정된 설명", "new-cover.jpg",
                    OrganizationType.ENTERPRISE, ActivityType.ONLINE,
                    "수정된 목적", "new@email.com", "01099999999",
                    ContactPreference.PHONE, "수정된 대상 설명",
                    List.of(2L, 3L), List.of(2L)
            );
        }

        @Test
        @DisplayName("성공: 2번 권한 보유자의 정보 수정")
        void success() {
            Organization org = createApprovedOrganization();
            OrganizationMember repMember = createRepMember(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockPermission(ORG_ID, USER_ID, repMember);

            organizationService.updateOrganization(USER_ID, ORG_ID, createUpdateParam());

            assertThat(org.getName()).isEqualTo("수정된 연합회");
            assertThat(org.getType()).isEqualTo(OrganizationType.ENTERPRISE);
        }

        @Test
        @DisplayName("실패: 승인되지 않은 연합회 수정")
        void failNotApproved() {
            Organization org = createPendingOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.updateOrganization(USER_ID, ORG_ID, createUpdateParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED));
        }

        @Test
        @DisplayName("실패: 권한 없는 유저")
        void failForbidden() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.updateOrganization(999L, ORG_ID, createUpdateParam()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.NOT_ORGANIZATION_MEMBER));
        }

        @Test
        @DisplayName("실패: 카테고리 미선택")
        void failNoCategoryIds() {
            Organization org = createApprovedOrganization();
            OrganizationMember repMember = createRepMember(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockPermission(ORG_ID, USER_ID, repMember);

            UpdateOrganizationParam param = new UpdateOrganizationParam(
                    "이름", null, null, OrganizationType.OTHER, ActivityType.BOTH,
                    "목적", "a@b.com", "010", ContactPreference.EMAIL, null,
                    List.of(), List.of(1L)
            );

            assertThatThrownBy(() -> organizationService.updateOrganization(USER_ID, ORG_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.CATEGORY_REQUIRED));
        }
    }

    @Nested
    @DisplayName("getOrganizationDetail - 연합회 단건 조회")
    class GetOrganizationDetail {

        @Test
        @DisplayName("성공: 승인된 연합회 상세 조회")
        void success() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            OrganizationDetailResult result = organizationService.getOrganizationDetail(ORG_ID);

            assertThat(result.id()).isEqualTo(ORG_ID);
            assertThat(result.name()).isEqualTo("테스트 연합회");
        }

        @Test
        @DisplayName("실패: 승인되지 않은 연합회 조회")
        void failNotApproved() {
            Organization org = createPendingOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.getOrganizationDetail(ORG_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 연합회 조회")
        void failNotFound() {
            given(organizationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.getOrganizationDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getOrganizationList - 연합회 목록 조회")
    class GetOrganizationList {

        @Test
        @DisplayName("성공: 필터 없이 목록 조회 (회원수 포함)")
        void successWithoutFilters() {
            List<OrganizationSummaryResult> content = List.of(
                    new OrganizationSummaryResult(1L, "서울대 연합회", null,
                            OrganizationType.UNIVERSITY, ActivityType.BOTH, "[1, 2]", "[1]", 50L),
                    new OrganizationSummaryResult(2L, "기업 연합회", "cover.jpg",
                            OrganizationType.ENTERPRISE, ActivityType.OFFLINE, "[3]", "[2]", 30L)
            );
            Slice<OrganizationSummaryResult> slice = new SliceImpl<>(content, PageRequest.of(0, 10), false);
            GetOrganizationListParam param = new GetOrganizationListParam(
                    null, null, null, null, PageRequest.of(0, 10));
            given(organizationRepository.findOrganizationList(any(GetOrganizationListParam.class)))
                    .willReturn(slice);

            Slice<OrganizationSummaryResult> result = organizationService.getOrganizationList(param);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).memberCount()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("requestDissolution - 연합회 해산 신청")
    class RequestDissolution {

        @Test
        @DisplayName("성공: 3번 권한 보유자 해산 신청")
        void success() {
            Organization org = createApprovedOrganization();
            OrganizationMember repMember = createRepMember(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockPermission(ORG_ID, USER_ID, repMember);

            organizationService.requestDissolution(USER_ID, ORG_ID, "운영 지속 불가");

            assertThat(org.getDissolutionStatus()).isEqualTo(DissolutionStatus.REQUESTED);
            assertThat(org.getDissolutionReason()).isEqualTo("운영 지속 불가");
        }

        @Test
        @DisplayName("실패: 권한 없는 유저")
        void failForbidden() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.requestDissolution(999L, ORG_ID, "사유"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.NOT_ORGANIZATION_MEMBER));
        }

        @Test
        @DisplayName("실패: 이미 해산 신청된 연합회")
        void failAlreadyRequested() {
            Organization org = createApprovedOrganization();
            org.requestDissolution("이전 사유");
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.requestDissolution(USER_ID, ORG_ID, "새 사유"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.DISSOLUTION_ALREADY_REQUESTED));
        }
    }

    @Nested
    @DisplayName("approveDissolution - 연합회 해산 승인")
    class ApproveDissolution {

        @Test
        @DisplayName("성공: 해산 승인 → 1일 유예 시작")
        void success() {
            Organization org = createApprovedOrganization();
            org.requestDissolution("사유");
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            organizationService.approveDissolution(ORG_ID);

            assertThat(org.getDissolutionStatus()).isEqualTo(DissolutionStatus.APPROVED);
            assertThat(org.getScheduledDeleteAt()).isAfter(org.getDissolutionApprovedAt());
        }

        @Test
        @DisplayName("실패: 해산 신청 상태가 아님")
        void failNotRequested() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.approveDissolution(ORG_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.DISSOLUTION_NOT_REQUESTED));
        }
    }

    @Nested
    @DisplayName("cancelDissolution - 연합회 해산 취소")
    class CancelDissolution {

        @Test
        @DisplayName("성공: 유예 기간 중 대표가 해산 취소")
        void success() {
            Organization org = createApprovedOrganization();
            org.requestDissolution("사유");
            org.approveDissolution();
            OrganizationMember repMember = createRepMember(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockPermission(ORG_ID, USER_ID, repMember);

            organizationService.cancelDissolution(USER_ID, ORG_ID);

            assertThat(org.getDissolutionStatus()).isEqualTo(DissolutionStatus.NONE);
            assertThat(org.getScheduledDeleteAt()).isNull();
        }

        @Test
        @DisplayName("실패: 제재 삭제는 취소 불가")
        void failSanctionDeletion() {
            Organization org = createApprovedOrganization();
            org.sanctionDeletion();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            assertThatThrownBy(() -> organizationService.cancelDissolution(USER_ID, ORG_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.DISSOLUTION_SANCTION_NOT_CANCELABLE));
        }

        @Test
        @DisplayName("실패: 유예 기간 만료 후 취소 시도")
        void failGracePeriodExpired() {
            Organization org = createApprovedOrganization();
            org.requestDissolution("사유");
            org.approveDissolution();
            ReflectionTestUtils.setField(org, "scheduledDeleteAt", LocalDateTime.now().minusHours(1));
            OrganizationMember repMember = createRepMember(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockPermission(ORG_ID, USER_ID, repMember);

            assertThatThrownBy(() -> organizationService.cancelDissolution(USER_ID, ORG_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.DISSOLUTION_NOT_IN_GRACE_PERIOD));
        }

        @Test
        @DisplayName("실패: 대표가 아닌 유저의 취소 시도")
        void failForbidden() {
            Organization org = createApprovedOrganization();
            org.requestDissolution("사유");
            org.approveDissolution();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.cancelDissolution(999L, ORG_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.NOT_ORGANIZATION_MEMBER));
        }
    }
}
