package com.dewple.organization.service;

import java.util.List;

public record CreateOrganizationRoleParam(
        String name,
        List<String> permissions,
        Boolean isStaff
) {
}
