package com.dewple.club.service;

import java.util.List;

public record CreateClubRoleParam(
        String name,
        List<String> permissions,
        Boolean isStaff
) {
}
