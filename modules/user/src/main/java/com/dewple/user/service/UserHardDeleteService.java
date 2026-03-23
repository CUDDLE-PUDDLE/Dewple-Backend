package com.dewple.user.service;

import com.dewple.common.entity.User;
import com.dewple.user.port.WithdrawalClubPort;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHardDeleteService {

    private final UserRepository userRepository;
    private final WithdrawalClubPort withdrawalClubPort;

    /**
     * deletedAt으로부터 7일 경과한 유저를 하드삭제.
     * app-worker 스케줄러에서 주기적으로 호출.
     */
    @Transactional
    public int hardDeleteExpiredUsers() {
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
        List<User> expiredUsers = userRepository.findByDeletedAtNotNullAndDeletedAtBefore(sevenDaysAgo);

        int count = 0;
        for (User user : expiredUsers) {
            withdrawalClubPort.removeFromAllClubs(user.getId());

            // TODO: 게시물/댓글 유저명 → '(알 수 없음)' (게시물/댓글 엔티티 미구현)
            // TODO: 지원서 응답 삭제
            // 별점(StarRating)은 받은 유저 종속 데이터이므로 삭제하지 않음 (3-7d)

            userRepository.delete(user);
            log.info("회원 하드삭제 완료: userId={}", user.getUserId());
            count++;
        }

        return count;
    }
}
