package com.dewple.activity.repository;

import com.dewple.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dewple.common.enums.ActivityLifecycleStatus;
import com.dewple.common.enums.BaseStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long>, ActivityRepositoryCustom {

    Optional<Activity> findByInviteCode(String inviteCode);

    Optional<Activity> findByManagerInviteCode(String managerInviteCode);

    List<Activity> findByCreatorIdAndStatus(Long creatorId, BaseStatus status);

    List<Activity> findByLifecycleStatusAndStartAtBefore(ActivityLifecycleStatus lifecycleStatus, OffsetDateTime startAt);
}
