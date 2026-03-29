package com.dewple.club.service;

import java.util.List;

public record InviteGuestParam(
        Long userId,
        List<Long> activityIds
) {
}
