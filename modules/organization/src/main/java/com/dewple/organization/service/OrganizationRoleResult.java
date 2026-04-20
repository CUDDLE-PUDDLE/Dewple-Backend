package com.dewple.organization.service;

import com.dewple.common.enums.OrganizationPermission;
import com.dewple.organization.entity.OrganizationRole;

import java.util.Arrays;
import java.util.List;

public record OrganizationRoleResult(
        Long id,
        String name,
        List<String> permissions,
        Boolean isStaff,
        Boolean isDefault
) {
    public static OrganizationRoleResult from(OrganizationRole role) {
        List<String> permissionNames = Arrays.stream(OrganizationPermission.values())
                .filter(role::hasPermission)
                .map(OrganizationPermission::name)
                .toList();

        return new OrganizationRoleResult(
                role.getId(),
                role.getName(),
                permissionNames,
                role.getIsStaff(),
                role.getIsDefault()
        );
    }
}
