package com.dewple.activity.repository;

import com.dewple.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dewple.common.enums.BaseStatus;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long>, ActivityRepositoryCustom {

    Optional<Activity> findByInviteCode(String inviteCode);

    List<Activity> findByCreatorIdAndStatus(Long creatorId, BaseStatus status);
}
