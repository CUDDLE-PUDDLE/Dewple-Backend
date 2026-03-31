package com.dewple.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.EditWindowBasis;
import com.dewple.common.enums.RecruitmentStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.recruitment.exception.RecruitmentErrorCode;
import com.dewple.club.entity.ClubGeneration;
import com.dewple.activity.entity.Activity;

@Entity
@Table(name = "recruitment_posting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentPosting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private ClubGeneration generation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb")
    private String content;

    @Column(name = "theme_color", length = 100)
    private String themeColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_window_basis", nullable = false, length = 20)
    private EditWindowBasis editWindowBasis;

    @Column(name = "edit_window_days", nullable = false)
    private Integer editWindowDays;

    @Column(name = "capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", nullable = false, length = 20)
    private RecruitmentStatus recruitmentStatus;

    @Column(name = "recent_recruitment_version", nullable = false)
    private Long recentRecruitmentVersion;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime endAt;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "end_of_generation_date")
    private LocalDate endOfGenerationDate;

    @Column(name = "has_second_interview", nullable = false)
    private Boolean hasSecondInterview = false;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;

    @Column(name = "deadline_change_count", nullable = false)
    private Integer deadlineChangeCount = 0;

    @Column(name = "extra_acceptance_end_date", columnDefinition = "timestamptz")
    private OffsetDateTime extraAcceptanceEndDate;

    @Column(name = "first_announcement_date")
    private LocalDate firstAnnouncementDate;

    @Column(name = "generation_start", nullable = false, length = 5)
    private String generationStart = "1";


    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentProcess> recruitmentProcesses = new ArrayList<>();

    @OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentDepartment> recruitmentDepartments = new ArrayList<>();


    @Builder
    public RecruitmentPosting(Club club, Activity activity, ClubGeneration generation,
                              User creator, String title, String content,
                              String themeColor, EditWindowBasis editWindowBasis,
                              Integer editWindowDays, Integer capacity,
                              RecruitmentStatus recruitmentStatus, Long recentRecruitmentVersion,
                              OffsetDateTime startAt, OffsetDateTime endAt,
                              LocalDate resultDate, LocalDate endOfGenerationDate,
                              Boolean hasSecondInterview, Long viewCount,
                              String emergencyContact, Integer deadlineChangeCount,
                              OffsetDateTime extraAcceptanceEndDate,
                              LocalDate firstAnnouncementDate, String generationStart) {
        validatePeriod(startAt, endAt);
        this.club = club;
        this.activity = activity;
        this.generation = generation;
        this.creator = creator;
        this.title = title;
        this.content = content;
        this.themeColor = themeColor;
        this.editWindowBasis = editWindowBasis;
        this.editWindowDays = editWindowDays;
        this.capacity = capacity;
        this.recruitmentStatus = recruitmentStatus;
        this.recentRecruitmentVersion = recentRecruitmentVersion;
        this.startAt = startAt;
        this.endAt = endAt;
        this.resultDate = resultDate;
        this.endOfGenerationDate = endOfGenerationDate;
        this.hasSecondInterview = hasSecondInterview != null ? hasSecondInterview : false;
        this.viewCount = viewCount != null ? viewCount : 0L;
        this.emergencyContact = emergencyContact;
        this.deadlineChangeCount = deadlineChangeCount != null ? deadlineChangeCount : 0;
        this.extraAcceptanceEndDate = extraAcceptanceEndDate;
        this.firstAnnouncementDate = firstAnnouncementDate;
        this.generationStart = generationStart != null ? generationStart : "1";
    }

    private void validatePeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (endAt != null && startAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("종료일시는 시작일시보다 늦어야 합니다");
        }
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void incrementVersion() {
        this.recentRecruitmentVersion++;
    }

    public void setPublishRecruitmentVersion() {
        this.recentRecruitmentVersion = 1L;
    }

    public void changeRecruitmentStatus(RecruitmentStatus status) {
        this.recruitmentStatus = status;
    }

    public void updateForDraft(String title, String content, ClubGeneration generation,
                               Integer capacity, OffsetDateTime startAt, OffsetDateTime endAt,
                               LocalDate resultDate, LocalDate endOfGenerationDate,
                               Boolean hasSecondInterview) {
        validatePeriod(startAt, endAt);
        this.title = title;
        this.content = content;
        this.generation = generation;
        this.capacity = capacity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.resultDate = resultDate;
        this.endOfGenerationDate = endOfGenerationDate;
        this.hasSecondInterview = hasSecondInterview;
    }

    private static final int MAX_DEADLINE_CHANGES = 2;

    public void updateHasSecondInterview(boolean hasSecondInterview) {
        this.hasSecondInterview = hasSecondInterview;
    }

    public void setExtraAcceptanceEndDate(OffsetDateTime extraAcceptanceEndDate) {
        this.extraAcceptanceEndDate = extraAcceptanceEndDate;
    }

    public void changeDeadline(OffsetDateTime newEndAt) {
        if (this.deadlineChangeCount >= MAX_DEADLINE_CHANGES) {
            throw new BusinessException(RecruitmentErrorCode.DEADLINE_CHANGE_LIMIT_EXCEEDED);
        }
        validatePeriod(this.startAt, newEndAt);
        this.endAt = newEndAt;
        this.deadlineChangeCount++;
    }

    // 연관관계 편의 메소드
    public void addRecruitmentProcess(RecruitmentProcess process) {
        this.recruitmentProcesses.add(process);
        process.changePosting(this);
    }

    public void removeRecruitmentProcess(RecruitmentProcess process) {
        this.recruitmentProcesses.remove(process);
        process.changePosting(null);
    }

    public void addRecruitmentDepartment(RecruitmentDepartment department) {
        this.recruitmentDepartments.add(department);
        department.changeRecruitment(this);
    }

    public void removeRecruitmentDepartment(RecruitmentDepartment department) {
        this.recruitmentDepartments.remove(department);
        department.changeRecruitment(null);
    }
}
