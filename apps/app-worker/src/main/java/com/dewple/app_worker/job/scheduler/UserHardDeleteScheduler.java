package com.dewple.app_worker.job.scheduler;

import com.dewple.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHardDeleteScheduler {

    private final UserService userService;

    /**
     * 1시간마다 실행.
     * deletedAt으로부터 7일 경과한 유저를 하드삭제.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void hardDeleteExpiredUsers() {
        int deleted = userService.hardDeleteExpiredUsers();
        if (deleted > 0) {
            log.info("회원 하드삭제 완료: {}건", deleted);
        }
    }
}
