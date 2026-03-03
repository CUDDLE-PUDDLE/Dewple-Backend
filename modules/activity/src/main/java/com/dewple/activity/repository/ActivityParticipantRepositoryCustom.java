package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.common.enums.BaseStatus;

import java.util.List;

public interface ActivityParticipantRepositoryCustom {

    List<ActivityParticipant> findByActivityIdAndStatusWithParticipant(Long activityId, BaseStatus status);
}
