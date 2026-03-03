package com.dewple.common.enums;

public enum Permission {
    PROPOSE_ACTIVITY(1L << 0),    // 1번 - "모임" 제안
    MANAGE_ACTIVITY(1L << 1),     // 2번 - "모임"(액티비티) 생성 및 수정
    EDIT_INFO(1L << 2),           // 3번 - 동아리 소개페이지, 태그 변경
    NETWORK_CHAT(1L << 3),        // 4번 - 동아리 네트워킹 (타 동아리 채팅)
    ANSWER_INQUIRY(1L << 4),      // 5번 - 문의 답변
    MANAGE_RECRUITMENT(1L << 5),  // 6번 - 지원서 제작, 수정, 열람
    DECIDE_ADMISSION(1L << 6),    // 7번 - 합격 여부 결정
    MANAGE_MEMBER(1L << 7),       // 8번 - 회원 관리 (역할 부여/초대/퇴출)
    MANAGE_FEDERATION(1L << 8);   // 9번 - 연합회 관리

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
