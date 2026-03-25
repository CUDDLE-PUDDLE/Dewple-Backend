package com.dewple.organization.repository;

import com.dewple.organization.entity.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRoleRepository extends JpaRepository<OrganizationRole, Long> {

    List<OrganizationRole> findByOrganizationId(Long organizationId);

    Optional<OrganizationRole> findByOrganizationIdAndName(Long organizationId, String name);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
