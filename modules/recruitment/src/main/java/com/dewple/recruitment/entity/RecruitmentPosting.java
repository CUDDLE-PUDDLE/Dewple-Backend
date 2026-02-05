package com.dewple.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.EditWindowBasis;
import com.dewple.common.enums.RecruitmentStatus;
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

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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


    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentProcess> recruitmentProcesses = new ArrayList<>();

    @OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentDepartment> recruitmentDepartments = new ArrayList<>();


    @Builder
    public RecruitmentPosting(Club club, Activity activity, ClubGeneration generation,
                              User creator, String title, String description,
                              String themeColor, EditWindowBasis editWindowBasis,
                              Integer editWindowDays, Integer capacity,
                              RecruitmentStatus recruitmentStatus, Long recentRecruitmentVersion,
                              OffsetDateTime startAt, OffsetDateTime endAt) {
        validatePeriod(startAt, endAt);
        this.club = club;
        this.activity = activity;
        this.generation = generation;
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.themeColor = themeColor;
        this.editWindowBasis = editWindowBasis;
        this.editWindowDays = editWindowDays;
        this.capacity = capacity;
        this.recruitmentStatus = recruitmentStatus;
        this.recentRecruitmentVersion = recentRecruitmentVersion;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    private void validatePeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (endAt != null && startAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("종료일시는 시작일시보다 늦어야 합니다");
        }
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
