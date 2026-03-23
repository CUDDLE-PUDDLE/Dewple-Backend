package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityNotice;
import com.dewple.common.enums.BaseStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityNoticeRepository extends JpaRepository<ActivityNotice, Long> {

    Slice<ActivityNotice> findByActivityIdAndStatusOrderByCreatedAtDesc(
            Long activityId, BaseStatus status, Pageable pageable);
}
