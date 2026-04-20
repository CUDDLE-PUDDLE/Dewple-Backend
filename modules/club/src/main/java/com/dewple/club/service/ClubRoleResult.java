package com.dewple.club.service;

import com.dewple.club.entity.ClubRole;
import com.dewple.common.enums.Permission;

import java.util.ArrayList;
import java.util.List;

public record ClubRoleResult(
        Long id,
        String name,
        List<String> permissions,
        Boolean isStaff,
        Boolean isDefault
) {
    public static ClubRoleResult from(ClubRole role) {
        List<String> permissionNames = new ArrayList<>();
        for (Permission p : Permission.values()) {
            if (role.hasPermission(p)) {
                permissionNames.add(p.name());
            }
        }
        return new ClubRoleResult(
                role.getId(), role.getName(), permissionNames,
                role.getIsStaff(), role.getIsDefault()
        );
    }
}
