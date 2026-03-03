package com.dewple.activity.service;

import org.springframework.data.domain.Pageable;

public record GetActivityListParam(
        ActivityListSection section,
        Pageable pageable
) {
}
