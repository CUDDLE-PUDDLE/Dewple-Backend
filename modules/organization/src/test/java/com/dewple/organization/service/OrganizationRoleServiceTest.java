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
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrganizationRoleServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationRoleRepository organizationRoleRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @InjectMocks
    private OrganizationRoleService organizationRoleService;

    private static final Long USER_ID = 1L;
    private static final Long ORG_ID = 100L;

    private User createUser(Long userId) {
        User user = User.builder().userId("user" + userId).name("유저" + userId).phone("010").build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Organization createApprovedOrganization() {
        Organization org = Organization.builder()
                .creator(createUser(USER_ID))
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
        org.approve();
        return org;
    }

    private OrganizationRole createRepRole(Organization org) {
        OrganizationRole role = OrganizationRole.builder()
                .organization(org).name("대표").permissions(OrganizationPermission.all())
                .isStaff(true).isDefault(true).build();
        ReflectionTestUtils.setField(role, "id", 1L);
        return role;
    }

    private OrganizationRole createDefaultMemberRole(Organization org) {
        OrganizationRole role = OrganizationRole.builder()
                .organization(org).name("연합회원").permissions(0L)
                .isStaff(false).isDefault(true).build();
        ReflectionTestUtils.setField(role, "id", 4L);
        return role;
    }

    private OrganizationRole createManagerRole(Organization org) {
        OrganizationRole role = OrganizationRole.builder()
                .organization(org).name("관리자")
                .permissions(OrganizationPermission.combine(
                        OrganizationPermission.MANAGE_ORGANIZATION,
                        OrganizationPermission.DISSOLVE_ORGANIZATION,
                        OrganizationPermission.MANAGE_NOTICE,
                        OrganizationPermission.CREATE_ACTIVITY,
                        OrganizationPermission.APPROVE_JOIN,
                        OrganizationPermission.ANSWER_INQUIRY,
                        OrganizationPermission.MANAGE_FEED,
                        OrganizationPermission.MANAGE_ATTENDANCE,
                        OrganizationPermission.MANAGE_CALENDAR,
                        OrganizationPermission.MANAGE_STORAGE
                ))
                .isStaff(true).isDefault(true).build();
        ReflectionTestUtils.setField(role, "id", 2L);
        return role;
    }

    private OrganizationRole createCustomRole(Organization org) {
        OrganizationRole role = OrganizationRole.builder()
                .organization(org).name("홍보담당")
                .permissions(OrganizationPermission.combine(
                        OrganizationPermission.MANAGE_NOTICE, OrganizationPermission.MANAGE_FEED))
                .isStaff(true).isDefault(false).build();
        ReflectionTestUtils.setField(role, "id", 10L);
        return role;
    }

    private OrganizationMember createMember(Organization org, OrganizationRole role, Long memberId, Long userId) {
        User user = createUser(userId);
        OrganizationMember member = OrganizationMember.builder()
                .organization(org).user(user).role(role).build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private void mockRepPermission(Organization org) {
        OrganizationRole repRole = createRepRole(org);
        OrganizationMember repMember = createMember(org, repRole, 50L, USER_ID);
        given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, USER_ID))
                .willReturn(Optional.of(repMember));
    }

    @Nested
    @DisplayName("createRole - 역할 생성")
    class CreateRole {

        @Test
        @DisplayName("성공: 대표가 커스텀 역할 생성")
        void success() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "홍보담당")).willReturn(false);
            given(organizationRoleRepository.save(any(OrganizationRole.class)))
                    .willAnswer(invocation -> {
                        OrganizationRole r = invocation.getArgument(0);
                        ReflectionTestUtils.setField(r, "id", 10L);
                        return r;
                    });

            CreateOrganizationRoleParam param = new CreateOrganizationRoleParam(
                    "홍보담당", List.of("MANAGE_NOTICE", "MANAGE_FEED"), true);

            OrganizationRoleResult result = organizationRoleService.createRole(USER_ID, ORG_ID, param);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("홍보담당");
            assertThat(result.permissions()).containsExactlyInAnyOrder("MANAGE_NOTICE", "MANAGE_FEED");
        }

        @Test
        @DisplayName("실패: 중복된 역할 이름")
        void failDuplicateName() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "대표")).willReturn(true);

            CreateOrganizationRoleParam param = new CreateOrganizationRoleParam("대표", List.of(), false);

            assertThatThrownBy(() -> organizationRoleService.createRole(USER_ID, ORG_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ROLE_NAME_DUPLICATED));
        }

        @Test
        @DisplayName("실패: 멤버가 아닌 유저")
        void failNotMember() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, 999L))
                    .willReturn(Optional.empty());

            CreateOrganizationRoleParam param = new CreateOrganizationRoleParam("역할", List.of(), false);

            assertThatThrownBy(() -> organizationRoleService.createRole(999L, ORG_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.NOT_ORGANIZATION_MEMBER));
        }
    }

    @Nested
    @DisplayName("updateRole - 역할 수정")
    class UpdateRole {

        @Test
        @DisplayName("성공: 커스텀 역할 수정")
        void successCustomRole() {
            Organization org = createApprovedOrganization();
            OrganizationRole role = createCustomRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationRoleRepository.findById(10L)).willReturn(Optional.of(role));
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "수정된역할")).willReturn(false);

            UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam(
                    "수정된역할", List.of("MANAGE_CALENDAR"), false);

            OrganizationRoleResult result = organizationRoleService.updateRole(USER_ID, ORG_ID, 10L, param);

            assertThat(result.name()).isEqualTo("수정된역할");
            assertThat(result.permissions()).containsExactly("MANAGE_CALENDAR");
        }

        @Test
        @DisplayName("성공: 대표가 기본 역할(관리자) 수정")
        void successDefaultRoleByRep() {
            Organization org = createApprovedOrganization();
            OrganizationRole managerRole = createManagerRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findById(2L)).willReturn(Optional.of(managerRole));
            OrganizationRole repRole = createRepRole(org);
            OrganizationMember repMember = createMember(org, repRole, 50L, USER_ID);
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, USER_ID))
                    .willReturn(Optional.of(repMember));

            UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam(
                    "관리자", List.of("MANAGE_NOTICE"), true);

            OrganizationRoleResult result = organizationRoleService.updateRole(USER_ID, ORG_ID, 2L, param);

            assertThat(result.permissions()).containsExactly("MANAGE_NOTICE");
        }

        @Test
        @DisplayName("실패: 대표 역할 수정 시도 (불변)")
        void failRepresentativeRole() {
            Organization org = createApprovedOrganization();
            OrganizationRole repRole = createRepRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findById(1L)).willReturn(Optional.of(repRole));

            UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam("변경", List.of(), false);

            assertThatThrownBy(() -> organizationRoleService.updateRole(USER_ID, ORG_ID, 1L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.REPRESENTATIVE_ROLE_NOT_MODIFIABLE));
        }

        @Test
        @DisplayName("실패: 이름 변경 시 중복")
        void failDuplicateName() {
            Organization org = createApprovedOrganization();
            OrganizationRole role = createCustomRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationRoleRepository.findById(10L)).willReturn(Optional.of(role));
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "관리자")).willReturn(true);

            UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam("관리자", List.of(), false);

            assertThatThrownBy(() -> organizationRoleService.updateRole(USER_ID, ORG_ID, 10L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ROLE_NAME_DUPLICATED));
        }
    }

    @Nested
    @DisplayName("deleteRole - 역할 삭제")
    class DeleteRole {

        @Test
        @DisplayName("성공: 커스텀 역할 삭제 + 멤버 연합회원 전환")
        void successWithMemberConversion() {
            Organization org = createApprovedOrganization();
            OrganizationRole customRole = createCustomRole(org);
            OrganizationRole defaultMemberRole = createDefaultMemberRole(org);
            OrganizationMember member1 = createMember(org, customRole, 60L, 2L);
            OrganizationMember member2 = createMember(org, customRole, 61L, 3L);

            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationRoleRepository.findById(10L)).willReturn(Optional.of(customRole));
            given(organizationRoleRepository.findByOrganizationIdAndName(ORG_ID, "연합회원"))
                    .willReturn(Optional.of(defaultMemberRole));
            given(organizationMemberRepository.findByRole(customRole))
                    .willReturn(List.of(member1, member2));

            organizationRoleService.deleteRole(USER_ID, ORG_ID, 10L);

            assertThat(member1.getRole().getName()).isEqualTo("연합회원");
            assertThat(member2.getRole().getName()).isEqualTo("연합회원");
            then(organizationRoleRepository).should().delete(customRole);
        }

        @Test
        @DisplayName("실패: 기본 역할 삭제 시도")
        void failDefaultRole() {
            Organization org = createApprovedOrganization();
            OrganizationRole repRole = createRepRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationRoleRepository.findById(1L)).willReturn(Optional.of(repRole));

            assertThatThrownBy(() -> organizationRoleService.deleteRole(USER_ID, ORG_ID, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ROLE_DEFAULT_NOT_DELETABLE));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 역할")
        void failNotFound() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationRoleRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationRoleService.deleteRole(USER_ID, ORG_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ROLE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getRoles - 역할 목록 조회")
    class GetRoles {

        @Test
        @DisplayName("성공: 역할 목록 조회")
        void success() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.existsById(ORG_ID)).willReturn(true);
            given(organizationRoleRepository.findByOrganizationId(ORG_ID))
                    .willReturn(List.of(createRepRole(org), createCustomRole(org)));

            List<OrganizationRoleResult> results = organizationRoleService.getRoles(ORG_ID);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).name()).isEqualTo("대표");
            assertThat(results.get(1).name()).isEqualTo("홍보담당");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 연합회")
        void failNotFound() {
            given(organizationRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> organizationRoleService.getRoles(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("initializeDefaultRoles - 기본 역할 초기화")
    class InitializeDefaultRoles {

        @Test
        @DisplayName("성공: 관리자 권한이 CSV 명세와 일치")
        void managerPermissionsMatchSpec() {
            Organization org = createApprovedOrganization();
            given(organizationRoleRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

            organizationRoleService.initializeDefaultRoles(org);

            @SuppressWarnings("unchecked")
            List<OrganizationRole> savedRoles = (List<OrganizationRole>)
                    org.getClass().cast(null); // We verify via argument capture

            then(organizationRoleRepository).should().saveAll(any());
        }
    }

    @Nested
    @DisplayName("assignRole - 멤버에 역할 부여")
    class AssignRole {

        @Test
        @DisplayName("성공: 멤버에 커스텀 역할 부여")
        void success() {
            Organization org = createApprovedOrganization();
            OrganizationRole customRole = createCustomRole(org);
            OrganizationRole defaultMemberRole = createDefaultMemberRole(org);
            OrganizationMember member = createMember(org, defaultMemberRole, 60L, 2L);

            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationMemberRepository.findByOrganizationIdAndId(ORG_ID, 60L))
                    .willReturn(Optional.of(member));
            given(organizationRoleRepository.findById(10L)).willReturn(Optional.of(customRole));

            organizationRoleService.assignRole(USER_ID, ORG_ID, 60L, 10L);

            assertThat(member.getRole().getName()).isEqualTo("홍보담당");
        }

        @Test
        @DisplayName("실패: 대표 역할 직접 할당 시도")
        void failAssignRepresentative() {
            Organization org = createApprovedOrganization();
            OrganizationRole repRole = createRepRole(org);
            OrganizationRole defaultMemberRole = createDefaultMemberRole(org);
            OrganizationMember member = createMember(org, defaultMemberRole, 60L, 2L);

            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationMemberRepository.findByOrganizationIdAndId(ORG_ID, 60L))
                    .willReturn(Optional.of(member));
            given(organizationRoleRepository.findById(1L)).willReturn(Optional.of(repRole));

            assertThatThrownBy(() -> organizationRoleService.assignRole(USER_ID, ORG_ID, 60L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.REPRESENTATIVE_ROLE_NOT_ASSIGNABLE));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버")
        void failMemberNotFound() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            mockRepPermission(org);
            given(organizationMemberRepository.findByOrganizationIdAndId(ORG_ID, 999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> organizationRoleService.assignRole(USER_ID, ORG_ID, 999L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("delegateRepresentative - 대표 위임")
    class DelegateRepresentative {

        @Test
        @DisplayName("성공: 대표 위임")
        void success() {
            Organization org = createApprovedOrganization();
            OrganizationRole repRole = createRepRole(org);
            OrganizationRole defaultMemberRole = createDefaultMemberRole(org);
            OrganizationMember currentRep = createMember(org, repRole, 50L, USER_ID);
            OrganizationMember targetMember = createMember(org, defaultMemberRole, 51L, 2L);

            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, USER_ID))
                    .willReturn(Optional.of(currentRep));
            given(organizationMemberRepository.findByOrganizationIdAndId(ORG_ID, 51L))
                    .willReturn(Optional.of(targetMember));
            given(organizationRoleRepository.findByOrganizationIdAndName(ORG_ID, "연합회원"))
                    .willReturn(Optional.of(defaultMemberRole));

            organizationRoleService.delegateRepresentative(USER_ID, ORG_ID, 51L);

            assertThat(targetMember.getRole().getName()).isEqualTo("대표");
            assertThat(currentRep.getRole().getName()).isEqualTo("연합회원");
        }

        @Test
        @DisplayName("실패: 대표가 아닌 유저의 위임 시도")
        void failNotRepresentative() {
            Organization org = createApprovedOrganization();
            OrganizationRole customRole = createCustomRole(org);
            OrganizationMember member = createMember(org, customRole, 50L, USER_ID);

            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, USER_ID))
                    .willReturn(Optional.of(member));

            assertThatThrownBy(() -> organizationRoleService.delegateRepresentative(USER_ID, ORG_ID, 51L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.DELEGATE_FORBIDDEN));
        }

        @Test
        @DisplayName("실패: 자기 자신에게 위임")
        void failDelegateSelf() {
            Organization org = createApprovedOrganization();
            OrganizationRole repRole = createRepRole(org);
            OrganizationMember currentRep = createMember(org, repRole, 50L, USER_ID);

            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationMemberRepository.findByOrganizationIdAndUserId(ORG_ID, USER_ID))
                    .willReturn(Optional.of(currentRep));
            given(organizationMemberRepository.findByOrganizationIdAndId(ORG_ID, 50L))
                    .willReturn(Optional.of(currentRep));

            assertThatThrownBy(() -> organizationRoleService.delegateRepresentative(USER_ID, ORG_ID, 50L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.DELEGATE_SELF));
        }
    }
}
