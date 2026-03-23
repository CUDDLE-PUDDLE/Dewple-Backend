package com.dewple.app_api_auth.infra.withdrawal;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ParticipantRole;
import com.dewple.common.enums.ParticipantStatus;
import com.dewple.user.port.WithdrawalActivityPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalActivityAdapter implements WithdrawalActivityPort {

    private final ActivityParticipantRepository activityParticipantRepository;
    private final ActivityRepository activityRepository;

    @Override
    public void cancelConfirmedParticipations(Long userId) {
        List<ActivityParticipant> participations =
                activityParticipantRepository.findByParticipantIdAndStatusAndParticipantStatusIn(
                        userId, BaseStatus.ACTIVE,
                        List.of(ParticipantStatus.CONFIRMED, ParticipantStatus.APPROVED));

        for (ActivityParticipant participant : participations) {
            participant.updateParticipantStatus(ParticipantStatus.DECLINED);
            participant.inactivate();

            // 선착순 모임이면 대기자 자동 승격
            Activity activity = participant.getActivity();
            if (!activity.getHasApplicationForm()) {
                activityParticipantRepository
                        .findFirstByActivityIdAndStatusAndParticipantStatusAndRoleOrderByWaitlistOrderAsc(
                                activity.getId(), BaseStatus.ACTIVE,
                                ParticipantStatus.PENDING, ParticipantRole.PARTICIPANT)
                        .ifPresent(next -> {
                            next.updateParticipantStatus(ParticipantStatus.CONFIRMED);
                            next.clearWaitlistOrder();
                            // TODO: 승격된 대기자에게 알림 (푸시+알림톡)
                            log.info("탈퇴로 인한 대기자 자동 승격: userId={}, activityId={}",
                                    next.getParticipant().getId(), activity.getId());
                        });
            }

            log.info("탈퇴로 인한 참여 취소: userId={}, activityId={}",
                    userId, activity.getId());
        }
    }

    @Override
    public void transferOrCancelLeaderActivities(Long userId) {
        List<Activity> leaderActivities =
                activityRepository.findByCreatorIdAndStatus(userId, BaseStatus.ACTIVE);

        for (Activity activity : leaderActivities) {
            List<ActivityParticipant> managers =
                    activityParticipantRepository.findByActivityIdAndStatusAndParticipantStatusAndRole(
                            activity.getId(), BaseStatus.ACTIVE,
                            ParticipantStatus.CONFIRMED, ParticipantRole.MANAGER);

            if (!managers.isEmpty()) {
                ActivityParticipant newLeader = managers.get(0);
                activity.changeCreator(newLeader.getParticipant());
                newLeader.updateRole(ParticipantRole.LEADER);
                log.info("모임장 승계: activityId={}, newLeaderId={}",
                        activity.getId(), newLeader.getParticipant().getId());
            } else {
                activity.inactivate();
                log.info("모임관리자 부재로 모임 삭제: activityId={}", activity.getId());
            }
        }
    }
}
