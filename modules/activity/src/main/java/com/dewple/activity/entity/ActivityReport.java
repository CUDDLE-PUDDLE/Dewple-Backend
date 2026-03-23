package com.dewple.activity.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ReportCategory;
import com.dewple.common.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "activity_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ReportCategory category;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, length = 20)
    private ReportStatus reportStatus = ReportStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    @Column(name = "resolved_at", columnDefinition = "timestamptz")
    private OffsetDateTime resolvedAt;

    @Builder
    public ActivityReport(Activity activity, User reporter, ReportCategory category,
                          String reason, String snapshot) {
        this.activity = activity;
        this.reporter = reporter;
        this.category = category;
        this.reason = reason;
        this.snapshot = snapshot;
    }

    public void resolve() {
        this.reportStatus = ReportStatus.RESOLVED;
        this.resolvedAt = OffsetDateTime.now();
    }

    public void dismiss() {
        this.reportStatus = ReportStatus.DISMISSED;
        this.resolvedAt = OffsetDateTime.now();
    }
}
