package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityReport;
import com.dewple.common.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityReportRepository extends JpaRepository<ActivityReport, Long> {

    Optional<ActivityReport> findTopByActivityIdAndReporterIdOrderByCreatedAtDesc(
            Long activityId, Long reporterId);

    List<ActivityReport> findByReportStatusAndResolvedAtBefore(
            ReportStatus status, OffsetDateTime before);
}
