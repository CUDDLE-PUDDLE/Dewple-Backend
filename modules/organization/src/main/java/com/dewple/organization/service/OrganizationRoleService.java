package com.dewple.organization.service;

import com.dewple.common.enums.ApprovalStatus;
import com.dewple.common.enums.OrganizationPermission;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.entity.OrganizationRole;
import com.dewple.organization.exception.OrganizationErrorCode;
import com.dewple.organization.repository.OrganizationRepository;
import com.dewple.organization.repository.OrganizationRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationRoleService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;

    @Transactional
    public OrganizationRoleResult createRole(Long userId, Long organizationId, CreateOrganizationRoleParam param) {
        Organization organization = findApprovedOrganization(organizationId);
        validateRoleManagePermission(organization, userId);

        if (organizationRoleRepository.existsByOrganizationIdAndName(organizationId, param.name())) {
            throw new BusinessException(OrganizationErrorCode.ROLE_NAME_DUPLICATED);
        }

        long permissionBits = convertPermissions(param.permissions());

        OrganizationRole role = OrganizationRole.builder()
                .organization(organization)
                .name(param.name())
                .permissions(permissionBits)
                .isStaff(param.isStaff())
                .isDefault(false)
                .build();

        organizationRoleRepository.save(role);
        log.info("연합회 역할 생성: organizationId={}, roleId={}, name={}", organizationId, role.getId(), param.name());
        return OrganizationRoleResult.from(role);
    }

    @Transactional
    public OrganizationRoleResult updateRole(Long userId, Long organizationId, Long roleId,
                                             UpdateOrganizationRoleParam param) {
        Organization organization = findApprovedOrganization(organizationId);
        validateRoleManagePermission(organization, userId);

        OrganizationRole role = organizationRoleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ROLE_NOT_FOUND));

        if (role.getIsDefault()) {
            throw new BusinessException(OrganizationErrorCode.ROLE_DEFAULT_NOT_MODIFIABLE);
        }

        if (!role.getName().equals(param.name())
                && organizationRoleRepository.existsByOrganizationIdAndName(organizationId, param.name())) {
            throw new BusinessException(OrganizationErrorCode.ROLE_NAME_DUPLICATED);
        }

        long permissionBits = convertPermissions(param.permissions());
        role.update(param.name(), permissionBits, param.isStaff());

        log.info("연합회 역할 수정: organizationId={}, roleId={}", organizationId, roleId);
        return OrganizationRoleResult.from(role);
    }

    @Transactional
    public void deleteRole(Long userId, Long organizationId, Long roleId) {
        Organization organization = findApprovedOrganization(organizationId);
        validateRoleManagePermission(organization, userId);

        OrganizationRole role = organizationRoleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ROLE_NOT_FOUND));

        if (role.getIsDefault()) {
            throw new BusinessException(OrganizationErrorCode.ROLE_DEFAULT_NOT_DELETABLE);
        }

        // TODO: 해당 역할이 부여된 회원은 연합회원 역할로 자동 전환
        organizationRoleRepository.delete(role);
        log.info("연합회 역할 삭제: organizationId={}, roleId={}", organizationId, roleId);
    }

    @Transactional(readOnly = true)
    public List<OrganizationRoleResult> getRoles(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND);
        }

        return organizationRoleRepository.findByOrganizationId(organizationId).stream()
                .map(OrganizationRoleResult::from)
                .toList();
    }

    @Transactional
    public void initializeDefaultRoles(Organization organization) {
        OrganizationRole representative = OrganizationRole.builder()
                .organization(organization)
                .name("대표")
                .permissions(OrganizationPermission.all())
                .isStaff(true)
                .isDefault(true)
                .build();

        OrganizationRole manager = OrganizationRole.builder()
                .organization(organization)
                .name("관리자")
                .permissions(OrganizationPermission.combine(
                        OrganizationPermission.MANAGE_ORGANIZATION,
                        OrganizationPermission.MANAGE_NOTICE,
                        OrganizationPermission.MANAGE_ROLE,
                        OrganizationPermission.APPROVE_JOIN,
                        OrganizationPermission.ANSWER_INQUIRY
                ))
                .isStaff(true)
                .isDefault(true)
                .build();

        OrganizationRole clubRepresentative = OrganizationRole.builder()
                .organization(organization)
                .name("동아리대표")
                .permissions(0L)
                .isStaff(false)
                .isDefault(true)
                .build();

        OrganizationRole member = OrganizationRole.builder()
                .organization(organization)
                .name("연합회원")
                .permissions(0L)
                .isStaff(false)
                .isDefault(true)
                .build();

        organizationRoleRepository.saveAll(List.of(representative, manager, clubRepresentative, member));
    }

    private Organization findApprovedOrganization(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        if (organization.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED);
        }
        return organization;
    }

    private void validateRoleManagePermission(Organization organization, Long userId) {
        // TODO: 역할/권한 시스템 완성 후 8번 권한(MANAGE_ROLE) 체크로 변경
        if (!organization.getCreator().getId().equals(userId)) {
            throw new BusinessException(OrganizationErrorCode.ROLE_MANAGE_FORBIDDEN);
        }
    }

    private long convertPermissions(List<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return 0L;
        }
        long bits = 0L;
        for (String name : permissionNames) {
            bits |= OrganizationPermission.valueOf(name).getValue();
        }
        return bits;
    }
}
