package com.dewple.common.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    WAITLISTED,
    TEMPORARY,
    FIX;

    /**
     * 운영진이 변경할 수 있는 허용된 상태 전이를 정의합니다.
     *
     * SUBMITTED  → ACCEPTED, REJECTED, WAITLISTED
     * REJECTED   → WAITLISTED (직접 ACCEPTED 불가)
     * WAITLISTED → ACCEPTED, REJECTED
     */
    public boolean canTransitionTo(ApplicationStatus target) {
        return getAllowedTransitions().contains(target);
    }

    private Set<ApplicationStatus> getAllowedTransitions() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(ACCEPTED, REJECTED, WAITLISTED);
            case REJECTED -> EnumSet.of(WAITLISTED);
            case WAITLISTED -> EnumSet.of(ACCEPTED, REJECTED);
            default -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }
}
