package com.dewple.organization.repository;

import com.dewple.organization.entity.OrganizationMember;
import com.dewple.organization.entity.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    Optional<OrganizationMember> findByOrganizationIdAndId(Long organizationId, Long memberId);

    List<OrganizationMember> findByRole(OrganizationRole role);

    long countByOrganizationId(Long organizationId);
}
