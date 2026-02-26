package com.dewple.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.enums.ProcessType;

@Entity
@Table(name = "recruitment_process")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentProcess extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_id", nullable = false)
    private RecruitmentPosting posting;

    @Column(name = "process_order", nullable = false)
    private Integer processOrder;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false, length = 100)
    private ProcessType processType;

    @Column(name = "start_at", columnDefinition = "timestamptz")
    private OffsetDateTime startAt;

    @Column(name = "end_at", columnDefinition = "timestamptz")
    private OffsetDateTime endAt;

    @Column(name = "interview_start_time")
    private LocalTime interviewStartTime;

    @Column(name = "interview_end_time")
    private LocalTime interviewEndTime;


    @OneToMany(mappedBy = "recruitmentProcess", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentSchema> recruitmentSchemas = new ArrayList<>();

    
    @Builder
    public RecruitmentProcess(RecruitmentPosting posting, Integer processOrder, String name,
                              String description, ProcessType processType,
                              OffsetDateTime startAt, OffsetDateTime endAt,
                              LocalTime interviewStartTime, LocalTime interviewEndTime) {
        
        validatePeriod(startAt, endAt);
        this.posting = posting;
        this.processOrder = processOrder;
        this.name = name;
        this.description = description;
        this.processType = processType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.interviewStartTime = interviewStartTime;
        this.interviewEndTime = interviewEndTime;
    }

    private void validatePeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (endAt != null && startAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("종료일시는 시작일시보다 늦어야 합니다");
        }
    }

    // 연관관계 편의 메소드
    void changePosting(RecruitmentPosting posting) {
        this.posting = posting;
    }

    public void addRecruitmentSchema(RecruitmentSchema schema) {
        this.recruitmentSchemas.add(schema);
        schema.changeRecruitmentProcess(this);
    }

    public void removeRecruitmentSchema(RecruitmentSchema schema) {
        this.recruitmentSchemas.remove(schema);
        schema.changeRecruitmentProcess(null);
    }
}
