package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.service.ParticipantResult;
import com.dewple.common.enums.BaseStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ActivityParticipantRepositoryCustom {

    List<ActivityParticipant> findByActivityIdAndStatusWithParticipant(Long activityId, BaseStatus status);

    Slice<ParticipantResult> findParticipantListByActivityId(Long activityId, Pageable pageable);
}
