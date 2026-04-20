package com.dewple.activity.service;

import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ParticipantRole;
import com.dewple.common.enums.ParticipantStatus;
import com.dewple.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityPermissionValidator {

    private final ActivityParticipantRepository participantRepository;

    public boolean isLeaderOrManager(Long activityId, Long userId) {
        return participantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .map(p -> p.getRole() == ParticipantRole.LEADER || p.getRole() == ParticipantRole.MANAGER)
                .orElse(false);
    }

    public void validateLeaderOrManager(Long activityId, Long userId) {
        if (!isLeaderOrManager(activityId, userId)) {
            throw new BusinessException(ActivityErrorCode.NOT_ACTIVITY_LEADER);
        }
    }

    public void validateConfirmedParticipant(Long activityId, Long userId) {
        boolean isConfirmed = participantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .map(p -> p.getParticipantStatus() == ParticipantStatus.CONFIRMED)
                .orElse(false);

        if (!isConfirmed) {
            throw new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_CONFIRMED);
        }
    }
}
