package com.dewple.organization.service;

import java.util.List;

public record UpdateOrganizationRoleParam(
        String name,
        List<String> permissions,
        Boolean isStaff
) {
}
