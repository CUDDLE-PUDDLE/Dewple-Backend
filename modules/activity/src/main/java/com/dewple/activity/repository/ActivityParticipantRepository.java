package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ParticipantStatus;

import java.util.List;
import java.util.Optional;

public interface ActivityParticipantRepository extends JpaRepository<ActivityParticipant, Long>, ActivityParticipantRepositoryCustom {

    List<ActivityParticipant> findByActivityId(Long activityId);

    Optional<ActivityParticipant> findByActivityIdAndParticipantId(Long activityId, Long participantId);

    long countByActivityIdAndStatusAndParticipantStatusIn(Long activityId, BaseStatus status, List<ParticipantStatus> participantStatuses);
}
