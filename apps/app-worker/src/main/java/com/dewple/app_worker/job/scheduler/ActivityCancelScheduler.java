package com.dewple.app_worker.job.scheduler;

import com.dewple.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityCancelScheduler {

    private final ActivityService activityService;

    /**
     * 5분마다 실행.
     * 모임 시작 시점이 지났는데 일반 참여자(PARTICIPANT)가 한 명도
     * CONFIRMED가 아닌 모임을 자동 취소한다.
     * capacity=0 모임은 자동 취소 대상에서 제외.
     */
    @Scheduled(fixedDelay = 300_000)
    public void cancelActivitiesWithNoParticipants() {
        int cancelled = activityService.cancelActivitiesAutomatically();
        if (cancelled > 0) {
            log.info("모임 자동 취소 완료: {}건", cancelled);
        }
    }
}
