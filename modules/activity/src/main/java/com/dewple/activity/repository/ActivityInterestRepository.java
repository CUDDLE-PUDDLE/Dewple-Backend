package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityInterest;
import com.dewple.common.enums.BaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityInterestRepository extends JpaRepository<ActivityInterest, Long> {

    Optional<ActivityInterest> findByActivityIdAndUserId(Long activityId, Long userId);

    long countByUserIdAndStatus(Long userId, BaseStatus status);
}
