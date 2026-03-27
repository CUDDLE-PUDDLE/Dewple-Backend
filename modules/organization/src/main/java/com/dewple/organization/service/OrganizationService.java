package com.dewple.organization.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.ApprovalStatus;
import com.dewple.common.enums.DissolutionStatus;
import com.dewple.common.enums.OrganizationPermission;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.entity.OrganizationMember;
import com.dewple.organization.entity.OrganizationRole;
import com.dewple.organization.exception.OrganizationErrorCode;
import com.dewple.organization.repository.OrganizationMemberRepository;
import com.dewple.organization.repository.OrganizationRepository;
import com.dewple.organization.repository.OrganizationRoleRepository;
import com.dewple.common.exception.CommonErrorCode;
import com.dewple.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationRoleService organizationRoleService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRoleRepository organizationRoleRepository;

    private static final String REPRESENTATIVE_ROLE_NAME = "대표";

    @Transactional
    public Organization apply(Long userId, CreateOrganizationParam param) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        if (param.categoryIds() == null || param.categoryIds().isEmpty()) {
            throw new BusinessException(OrganizationErrorCode.CATEGORY_REQUIRED);
        }
        if (param.categoryIds().size() > 3) {
            throw new BusinessException(OrganizationErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }
        if (param.regionIds() == null || param.regionIds().isEmpty()) {
            throw new BusinessException(OrganizationErrorCode.REGION_REQUIRED);
        }

        String targetClubIds = param.targetClubIds() != null && !param.targetClubIds().isEmpty()
                ? param.targetClubIds().toString() : null;

        Organization organization = Organization.builder()
                .creator(creator)
                .name(param.name())
                .purpose(param.purpose())
                .type(param.type())
                .activityType(param.activityType())
                .contactEmail(param.contactEmail())
                .contactPhone(param.contactPhone())
                .contactPreference(param.contactPreference())
                .targetClubsDescription(param.targetClubsDescription())
                .targetClubIds(targetClubIds)
                .categoryIds(param.categoryIds().toString())
                .regionIds(param.regionIds().toString())
                .build();

        organizationRepository.save(organization);
        log.info("연합회 생성 신청: organizationId={}, userId={}", organization.getId(), userId);
        return organization;
    }

    @Transactional
    public void approve(Long organizationId) {
        Organization organization = findById(organizationId);

        if (organization.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_PENDING);
        }

        organization.approve();

        organizationRoleService.initializeDefaultRoles(organization);

        OrganizationRole representativeRole = organizationRoleRepository
                .findByOrganizationIdAndName(organizationId, REPRESENTATIVE_ROLE_NAME)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ROLE_NOT_FOUND));

        OrganizationMember creatorMember = OrganizationMember.builder()
                .organization(organization)
                .user(organization.getCreator())
                .role(representativeRole)
                .build();
        organizationMemberRepository.save(creatorMember);

        log.info("연합회 생성 승인: organizationId={}, creatorMemberId={}", organizationId, creatorMember.getId());
    }

    @Transactional
    public void reject(Long organizationId, String reason) {
        Organization organization = findById(organizationId);

        if (organization.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_PENDING);
        }

        organization.reject(reason);
        log.info("연합회 생성 반려: organizationId={}, reason={}", organizationId, reason);
    }

    @Transactional(readOnly = true)
    public List<Organization> getPendingApplications() {
        return organizationRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Slice<OrganizationSummaryResult> getOrganizationList(GetOrganizationListParam param) {
        return organizationRepository.findOrganizationList(param);
    }

    @Transactional(readOnly = true)
    public OrganizationDetailResult getOrganizationDetail(Long organizationId) {
        Organization organization = findById(organizationId);

        if (organization.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED);
        }

        return OrganizationDetailResult.from(organization);
    }

    @Transactional
    public void updateOrganization(Long userId, Long organizationId, UpdateOrganizationParam param) {
        Organization organization = findById(organizationId);

        if (organization.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED);
        }

        validatePermission(organizationId, userId, OrganizationPermission.MANAGE_ORGANIZATION);

        if (param.categoryIds() == null || param.categoryIds().isEmpty()) {
            throw new BusinessException(OrganizationErrorCode.CATEGORY_REQUIRED);
        }
        if (param.categoryIds().size() > 3) {
            throw new BusinessException(OrganizationErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }
        if (param.regionIds() == null || param.regionIds().isEmpty()) {
            throw new BusinessException(OrganizationErrorCode.REGION_REQUIRED);
        }

        organization.update(
                param.name(), param.description(), param.coverImg(),
                param.type(), param.activityType(), param.purpose(),
                param.contactEmail(), param.contactPhone(), param.contactPreference(),
                param.targetClubsDescription(),
                param.categoryIds().toString(), param.regionIds().toString()
        );

        log.info("연합회 정보 수정: organizationId={}, userId={}", organizationId, userId);
    }

    @Transactional
    public void requestDissolution(Long userId, Long organizationId, String reason) {
        Organization organization = findById(organizationId);

        if (organization.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_APPROVED);
        }

        if (organization.getDissolutionStatus() != DissolutionStatus.NONE) {
            throw new BusinessException(OrganizationErrorCode.DISSOLUTION_ALREADY_REQUESTED);
        }

        validatePermission(organizationId, userId, OrganizationPermission.DISSOLVE_ORGANIZATION);

        organization.requestDissolution(reason);
        log.info("연합회 해산 신청: organizationId={}, userId={}", organizationId, userId);
    }

    @Transactional
    public void approveDissolution(Long organizationId) {
        Organization organization = findById(organizationId);

        if (organization.getDissolutionStatus() != DissolutionStatus.REQUESTED) {
            throw new BusinessException(OrganizationErrorCode.DISSOLUTION_NOT_REQUESTED);
        }

        organization.approveDissolution();
        log.info("연합회 해산 승인: organizationId={}, scheduledDeleteAt={}",
                organizationId, organization.getScheduledDeleteAt());
    }

    @Transactional
    public void cancelDissolution(Long userId, Long organizationId) {
        Organization organization = findById(organizationId);

        if (organization.getDissolutionStatus() != DissolutionStatus.APPROVED) {
            throw new BusinessException(OrganizationErrorCode.DISSOLUTION_NOT_IN_GRACE_PERIOD);
        }

        if (organization.getIsSanctionDeletion()) {
            throw new BusinessException(OrganizationErrorCode.DISSOLUTION_SANCTION_NOT_CANCELABLE);
        }

        validateRepresentative(organizationId, userId);

        if (organization.getScheduledDeleteAt() != null
                && LocalDateTime.now().isAfter(organization.getScheduledDeleteAt())) {
            throw new BusinessException(OrganizationErrorCode.DISSOLUTION_NOT_IN_GRACE_PERIOD);
        }

        organization.cancelDissolution();
        log.info("연합회 해산 취소: organizationId={}, userId={}", organizationId, userId);
    }

    @Transactional(readOnly = true)
    public Organization getById(Long organizationId) {
        return findById(organizationId);
    }

    private Organization findById(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
    }

    private void validatePermission(Long organizationId, Long userId, OrganizationPermission permission) {
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.NOT_ORGANIZATION_MEMBER));

        if (!member.getRole().hasPermission(permission)) {
            throw new BusinessException(OrganizationErrorCode.PERMISSION_DENIED);
        }
    }

    private void validateRepresentative(Long organizationId, Long userId) {
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.NOT_ORGANIZATION_MEMBER));

        OrganizationRole role = member.getRole();
        if (!REPRESENTATIVE_ROLE_NAME.equals(role.getName()) || !role.getIsDefault()) {
            throw new BusinessException(OrganizationErrorCode.DISSOLUTION_CANCEL_FORBIDDEN);
        }
    }
}
