package com.dewple.organization.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.*;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.entity.OrganizationRole;
import com.dewple.organization.exception.OrganizationErrorCode;
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

    @InjectMocks
    private OrganizationRoleService organizationRoleService;

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

    private Organization createApprovedOrganization() {
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
        org.approve();
        return org;
    }

    private OrganizationRole createCustomRole(Organization org) {
        OrganizationRole role = OrganizationRole.builder()
                .organization(org)
                .name("홍보담당")
                .permissions(OrganizationPermission.combine(
                        OrganizationPermission.MANAGE_NOTICE, OrganizationPermission.MANAGE_FEED))
                .isStaff(true)
                .isDefault(false)
                .build();
        ReflectionTestUtils.setField(role, "id", 10L);
        return role;
    }

    private OrganizationRole createDefaultRole(Organization org) {
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

    @Nested
    @DisplayName("createRole - 역할 생성")
    class CreateRole {

        @Test
        @DisplayName("성공: 커스텀 역할 생성")
        void success() {
            // given
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "홍보담당")).willReturn(false);
            given(organizationRoleRepository.save(any(OrganizationRole.class)))
                    .willAnswer(invocation -> {
                        OrganizationRole r = invocation.getArgument(0);
                        ReflectionTestUtils.setField(r, "id", 10L);
                        return r;
                    });

            CreateOrganizationRoleParam param = new CreateOrganizationRoleParam(
                    "홍보담당", List.of("MANAGE_NOTICE", "MANAGE_FEED"), true
            );

            // when
            OrganizationRoleResult result = organizationRoleService.createRole(USER_ID, ORG_ID, param);

            // then
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("홍보담당");
            assertThat(result.permissions()).containsExactlyInAnyOrder("MANAGE_NOTICE", "MANAGE_FEED");
            assertThat(result.isStaff()).isTrue();
            assertThat(result.isDefault()).isFalse();
        }

        @Test
        @DisplayName("실패: 중복된 역할 이름")
        void failDuplicateName() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "대표")).willReturn(true);

            CreateOrganizationRoleParam param = new CreateOrganizationRoleParam(
                    "대표", List.of(), false
            );

            assertThatThrownBy(() -> organizationRoleService.createRole(USER_ID, ORG_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ROLE_NAME_DUPLICATED));
        }

        @Test
        @DisplayName("실패: 권한 없는 유저")
        void failForbidden() {
            Organization org = createApprovedOrganization();
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));

            CreateOrganizationRoleParam param = new CreateOrganizationRoleParam(
                    "역할", List.of(), false
            );

            assertThatThrownBy(() -> organizationRoleService.createRole(999L, ORG_ID, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ROLE_MANAGE_FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("updateRole - 역할 수정")
    class UpdateRole {

        @Test
        @DisplayName("성공: 커스텀 역할 수정")
        void success() {
            Organization org = createApprovedOrganization();
            OrganizationRole role = createCustomRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findById(10L)).willReturn(Optional.of(role));
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "수정된역할")).willReturn(false);

            UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam(
                    "수정된역할", List.of("MANAGE_CALENDAR"), false
            );

            OrganizationRoleResult result = organizationRoleService.updateRole(USER_ID, ORG_ID, 10L, param);

            assertThat(result.name()).isEqualTo("수정된역할");
            assertThat(result.permissions()).containsExactly("MANAGE_CALENDAR");
            assertThat(result.isStaff()).isFalse();
        }

        @Test
        @DisplayName("실패: 기본 역할 수정 시도")
        void failDefaultRole() {
            Organization org = createApprovedOrganization();
            OrganizationRole role = createDefaultRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findById(1L)).willReturn(Optional.of(role));

            UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam(
                    "변경", List.of(), false
            );

            assertThatThrownBy(() -> organizationRoleService.updateRole(USER_ID, ORG_ID, 1L, param))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(OrganizationErrorCode.ROLE_DEFAULT_NOT_MODIFIABLE));
        }

        @Test
        @DisplayName("실패: 이름 변경 시 중복")
        void failDuplicateName() {
            Organization org = createApprovedOrganization();
            OrganizationRole role = createCustomRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findById(10L)).willReturn(Optional.of(role));
            given(organizationRoleRepository.existsByOrganizationIdAndName(ORG_ID, "관리자")).willReturn(true);

            UpdateOrganizationRoleParam param = new UpdateOrganizationRoleParam(
                    "관리자", List.of(), false
            );

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
        @DisplayName("성공: 커스텀 역할 삭제")
        void success() {
            Organization org = createApprovedOrganization();
            OrganizationRole role = createCustomRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findById(10L)).willReturn(Optional.of(role));

            organizationRoleService.deleteRole(USER_ID, ORG_ID, 10L);

            then(organizationRoleRepository).should().delete(role);
        }

        @Test
        @DisplayName("실패: 기본 역할 삭제 시도")
        void failDefaultRole() {
            Organization org = createApprovedOrganization();
            OrganizationRole role = createDefaultRole(org);
            given(organizationRepository.findById(ORG_ID)).willReturn(Optional.of(org));
            given(organizationRoleRepository.findById(1L)).willReturn(Optional.of(role));

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
                    .willReturn(List.of(createDefaultRole(org), createCustomRole(org)));

            List<OrganizationRoleResult> results = organizationRoleService.getRoles(ORG_ID);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).name()).isEqualTo("대표");
            assertThat(results.get(0).isDefault()).isTrue();
            assertThat(results.get(1).name()).isEqualTo("홍보담당");
            assertThat(results.get(1).isDefault()).isFalse();
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
}
