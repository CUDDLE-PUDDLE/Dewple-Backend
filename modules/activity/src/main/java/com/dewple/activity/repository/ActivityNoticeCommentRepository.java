package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityNoticeComment;
import com.dewple.common.enums.BaseStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityNoticeCommentRepository extends JpaRepository<ActivityNoticeComment, Long> {

    Slice<ActivityNoticeComment> findByNoticeIdAndStatusOrderByCreatedAtAsc(
            Long noticeId, BaseStatus status, Pageable pageable);
}
