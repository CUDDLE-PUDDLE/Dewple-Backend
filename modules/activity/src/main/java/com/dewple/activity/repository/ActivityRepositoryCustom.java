package com.dewple.activity.repository;

import com.dewple.activity.service.ActivitySummaryResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ActivityRepositoryCustom {

    Slice<ActivitySummaryResult> findPersonalActivities(Long userId, Pageable pageable);

    Slice<ActivitySummaryResult> findActivitiesByLikedClubs(Long userId, Pageable pageable);

    Slice<ActivitySummaryResult> findActivitiesByMyClubs(Long userId, Pageable pageable);
}
