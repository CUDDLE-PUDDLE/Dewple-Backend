package com.dewple.organization.repository;

import com.dewple.common.enums.ApprovalStatus;
import com.dewple.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findByApprovalStatus(ApprovalStatus approvalStatus);
}
