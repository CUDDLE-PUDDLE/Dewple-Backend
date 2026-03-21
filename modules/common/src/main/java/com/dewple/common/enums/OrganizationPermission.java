package com.dewple.common.enums;

public enum OrganizationPermission {
    DELEGATE_REPRESENTATIVE(1L << 0),   // 1번 - 대표위임 (대표에게만 부여 가능)
    MANAGE_ORGANIZATION(1L << 1),       // 2번 - 연합회관리 (정보 수정/설정 변경)
    DISSOLVE_ORGANIZATION(1L << 2),     // 3번 - 연합회해산
    MANAGE_NOTICE(1L << 3),             // 4번 - 공지관리 (생성/수정/삭제/댓글 삭제)
    CREATE_ACTIVITY(1L << 4),           // 5번 - 모임생성
    APPROVE_JOIN(1L << 5),              // 6번 - 가입신청 승인/거부
    EXPEL_CLUB(1L << 6),               // 7번 - 소속동아리 제명
    MANAGE_ROLE(1L << 7),              // 8번 - 역할관리 (역할 생성/수정, 권한 추가/삭제)
    ANSWER_INQUIRY(1L << 8),           // 9번 - 문의답변
    MANAGE_FEED(1L << 9),              // 10번 - 피드관리 (생성/수정/삭제)
    MANAGE_ATTENDANCE(1L << 10),       // 11번 - 출석관리 (출석부 관리/출석 데이터 열람)
    MANAGE_CALENDAR(1L << 11),         // 12번 - 일정관리 (캘린더 일정 생성/수정/삭제)
    MANAGE_STORAGE(1L << 12),          // 13번 - 자료실관리 (폴더 권한/파일 업로드/삭제)
    MANAGE_BUDGET(1L << 13);           // 14번 - 예산관리 (스토리지 추가 구매)

    private final long value;

    OrganizationPermission(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public static long combine(OrganizationPermission... permissions) {
        long result = 0;
        for (OrganizationPermission permission : permissions) {
            result |= permission.value;
        }
        return result;
    }

    public static long all() {
        long result = 0;
        for (OrganizationPermission permission : values()) {
            result |= permission.value;
        }
        return result;
    }
}
