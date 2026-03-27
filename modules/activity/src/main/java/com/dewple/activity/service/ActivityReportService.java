package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.entity.ActivityReport;
import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.activity.repository.ActivityReportRepository;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ParticipantStatus;
import com.dewple.common.enums.ReportCategory;
import com.dewple.common.enums.ReportStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.common.exception.CommonErrorCode;
import com.dewple.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityReportService {

    private static final int REPORT_COOLDOWN_HOURS = 168; // 7일

    private final ActivityRepository activityRepository;
    private final ActivityReportRepository reportRepository;
    private final ActivityParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional
    public ActivityReport reportActivity(Long userId, Long activityId, ReportCategory category, String reason) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        // 중복 신고 체크 (동일 대상, 마지막 신고 후 7일 이내)
        reportRepository.findTopByActivityIdAndReporterIdOrderByCreatedAtDesc(activityId, userId)
                .ifPresent(lastReport -> {
                    OffsetDateTime cooldownEnd = lastReport.getCreatedAt()
                            .plusHours(REPORT_COOLDOWN_HOURS);
                    if (OffsetDateTime.now().isBefore(cooldownEnd)) {
                        throw new BusinessException(ActivityErrorCode.REPORT_COOLDOWN);
                    }
                });

        // 모임 정보 스냅샷 생성
        String snapshot = createSnapshot(activity);

        ActivityReport report = ActivityReport.builder()
                .activity(activity)
                .reporter(reporter)
                .category(category)
                .reason(reason)
                .snapshot(snapshot)
                .build();

        reportRepository.save(report);
        // TODO: 신고자에게 '접수되었습니다' 알림 발송
        log.info("모임 신고 접수: reportId={}, activityId={}, by userId={}", report.getId(), activityId, userId);
        return report;
    }

    /**
     * 신고 처리 완료 후 6개월 경과한 스냅샷 자동 삭제.
     * app-worker 스케줄러에서 주기적으로 호출.
     */
    @Transactional
    public int deleteExpiredSnapshots() {
        OffsetDateTime sixMonthsAgo = OffsetDateTime.now().minusMonths(6);

        List<ActivityReport> resolved = reportRepository.findByReportStatusAndResolvedAtBefore(
                ReportStatus.RESOLVED, sixMonthsAgo);
        List<ActivityReport> dismissed = reportRepository.findByReportStatusAndResolvedAtBefore(
                ReportStatus.DISMISSED, sixMonthsAgo);

        int count = 0;
        for (ActivityReport report : resolved) {
            report.inactivate();
            count++;
        }
        for (ActivityReport report : dismissed) {
            report.inactivate();
            count++;
        }

        if (count > 0) {
            log.info("만료 신고 스냅샷 삭제: {}건", count);
        }
        return count;
    }

    private String createSnapshot(Activity activity) {
        long participantCount = participantRepository.countByActivityIdAndStatusAndParticipantStatusIn(
                activity.getId(), BaseStatus.ACTIVE,
                List.of(ParticipantStatus.CONFIRMED, ParticipantStatus.APPROVED));

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"title\":\"").append(escape(activity.getName())).append("\"");
        sb.append(",\"description\":\"").append(escape(activity.getDescription())).append("\"");
        sb.append(",\"creatorId\":").append(activity.getCreator().getId());
        sb.append(",\"creatorNickname\":\"").append(escape(activity.getCreator().getNickname())).append("\"");
        sb.append(",\"startAt\":\"").append(activity.getStartAt()).append("\"");
        sb.append(",\"endAt\":\"").append(activity.getEndAt()).append("\"");
        sb.append(",\"openType\":\"").append(activity.getOpenType().name()).append("\"");
        sb.append(",\"participantCount\":").append(participantCount);

        if (activity.getClub() != null) {
            sb.append(",\"createdBy\":\"CLUB\"");
            sb.append(",\"clubId\":").append(activity.getClub().getId());
            sb.append(",\"clubName\":\"").append(escape(activity.getClub().getName())).append("\"");
        } else if (activity.getOrganization() != null) {
            sb.append(",\"createdBy\":\"ORGANIZATION\"");
            sb.append(",\"organizationId\":").append(activity.getOrganization().getId());
            sb.append(",\"organizationName\":\"").append(escape(activity.getOrganization().getName())).append("\"");
        } else {
            sb.append(",\"createdBy\":\"PERSONAL\"");
        }

        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
