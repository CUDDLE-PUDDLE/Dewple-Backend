package com.dewple.common.enums;

public enum Permission {
    PROPOSE_ACTIVITY(1L << 0),      // 1번 - 모임제안
    MANAGE_ACTIVITY(1L << 1),       // 2번 - 모임생성
    EDIT_INFO(1L << 2),             // 3번 - 동아리관리 (소개페이지/태그/설정 변경)
    NETWORK_CHAT(1L << 3),          // 4번 - 네트워킹 (타 동아리 채팅)
    ANSWER_INQUIRY(1L << 4),        // 5번 - 문의답변
    MANAGE_RECRUITMENT(1L << 5),    // 6번 - 지원서 제작·수정 (답변 열람 권한 없음)
    DECIDE_ADMISSION(1L << 6),      // 7번 - 지원자합격결정 (8번 답변열람 자동 포함)
    VIEW_APPLICATION(1L << 7),      // 8번 - 지원서답변열람 (단독 부여 가능)
    MANAGE_MEMBER(1L << 8),         // 9번 - 회원관리 (역할부여/초대/내보내기)
    MANAGE_FEDERATION(1L << 9),     // 10번 - 연합회관리 (가입신청/탈퇴/연합회 공지 수신)
    DELETE_CLUB(1L << 10),          // 11번 - 동아리삭제
    DELEGATE_PRESIDENT(1L << 11),   // 12번 - 회장위임 (회장에게만 부여 가능)
    MANAGE_NOTICE(1L << 12),        // 13번 - 공지관리 (생성/수정/삭제/댓글 삭제)
    MANAGE_FEED(1L << 13),          // 14번 - 피드관리 (생성/수정/삭제)
    MANAGE_ATTENDANCE(1L << 14),    // 15번 - 출석관리 (출석부 관리/출석 데이터 열람)
    MANAGE_CALENDAR(1L << 15),      // 16번 - 일정관리 (캘린더 일정 생성/수정/삭제)
    MANAGE_STORAGE(1L << 16),       // 17번 - 자료실관리 (폴더 권한/파일 업로드/삭제)
    MANAGE_BUDGET(1L << 17);        // 18번 - 예산관리 (스토리지 추가 구매)

    private final long value;

    Permission(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    // 여러 권한을 조합하여 비트마스크 값을 반환
    public static long combine(Permission... permissions) {
        long result = 0;
        for (Permission permission : permissions) {
            result |= permission.value;
        }
        return result;
    }

    // 모든 권한을 가진 비트마스크 값 반환
    public static long all() {
        long result = 0;
        for (Permission permission : values()) {
            result |= permission.value;
        }
        return result;
    }
}
