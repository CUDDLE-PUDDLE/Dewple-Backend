package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityInquiry;
import com.dewple.common.enums.BaseStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityInquiryRepository extends JpaRepository<ActivityInquiry, Long> {

    Slice<ActivityInquiry> findByActivityIdAndStatusOrderByCreatedAtDesc(
            Long activityId, BaseStatus status, Pageable pageable);

    long countByActivityIdAndStatus(Long activityId, BaseStatus status);
}
