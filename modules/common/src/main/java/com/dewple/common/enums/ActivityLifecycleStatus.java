package com.dewple.common.enums;

public enum ActivityLifecycleStatus {
    RECRUITING,   // 모집 중
    IN_PROGRESS,  // 진행 중 (모임 기간 내)
    ENDED,        // 종료 (1주일 뒤 삭제 대기, 별점 평가 기간)
    CANCELLED,    // 취소됨 (자동/수동/제재 — 이력에 안 남음)
    DELETED       // 삭제 완료 (종료 후 1주일 경과)
}
