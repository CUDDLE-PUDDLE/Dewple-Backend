package com.dewple.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "application"
//, uniqueConstraints = { @UniqueConstraint(columnNames = {"recruitment_schema_id", "applicant_id"}) }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_schema_id", nullable = false)
    private RecruitmentSchema recruitmentSchema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", nullable = false, columnDefinition = "jsonb")
    private String answers = "[]";

    @Column(name = "interview_start_date")
    private LocalDateTime interviewStartDate;

    @Column(name = "interview_location", length = 255)
    private String interviewLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 20)
    private ApplicationStatus applicationStatus;

    @Builder
    public Application(RecruitmentSchema recruitmentSchema, User applicant, String answers,
                       LocalDateTime interviewStartDate, String interviewLocation,
                       ApplicationStatus applicationStatus) {
        this.recruitmentSchema = recruitmentSchema;
        this.applicant = applicant;
        this.answers = answers != null ? answers : "[]";
        this.interviewStartDate = interviewStartDate;
        this.interviewLocation = interviewLocation;
        this.applicationStatus = applicationStatus;
    }
}
