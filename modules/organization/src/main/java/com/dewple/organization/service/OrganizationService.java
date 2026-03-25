package com.dewple.organization.service;

import com.dewple.common.entity.User;
import com.dewple.common.enums.ApprovalStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.organization.entity.Organization;
import com.dewple.organization.exception.OrganizationErrorCode;
import com.dewple.organization.repository.OrganizationRepository;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.data.domain.Slice;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Organization apply(Long userId, CreateOrganizationParam param) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (param.categoryIds() == null || param.categoryIds().isEmpty()) {
            throw new BusinessException(OrganizationErrorCode.CATEGORY_REQUIRED);
        }
        if (param.categoryIds().size() > 3) {
            throw new BusinessException(OrganizationErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }
        if (param.regionIds() == null || param.regionIds().isEmpty()) {
            throw new BusinessException(OrganizationErrorCode.REGION_REQUIRED);
        }

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
        // TODO: 신청자에게 승인 알림 발송
        log.info("연합회 생성 승인: organizationId={}", organizationId);
    }

    @Transactional
    public void reject(Long organizationId, String reason) {
        Organization organization = findById(organizationId);

        if (organization.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_PENDING);
        }

        organization.reject(reason);
        // TODO: 신청자에게 반려 알림 발송 (반려 사유 포함)
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
    public Organization getById(Long organizationId) {
        return findById(organizationId);
    }

    private Organization findById(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
    }
}
