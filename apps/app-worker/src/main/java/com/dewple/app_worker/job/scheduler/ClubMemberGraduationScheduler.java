package com.dewple.app_worker.job.scheduler;

import com.dewple.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClubMemberGraduationScheduler {

    private final ClubService clubService;

    /**
     * 매일 자정에 실행.
     * 활동 종료일이 지난 ACTIVE 멤버를 자동으로 GRADUATED 처리.
     * GUEST는 수료 대상에서 제외 (ACTIVE 상태만 조회).
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void graduateExpiredMembers() {
        int graduated = clubService.processGraduation();
        if (graduated > 0) {
            log.info("동아리 멤버 자동 수료 처리 완료: {}명", graduated);
        }
    }
}
