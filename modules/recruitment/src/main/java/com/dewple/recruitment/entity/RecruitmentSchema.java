package com.dewple.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.dewple.common.entity.BaseEntity;

@Entity
@Table(name = "recruitment_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentSchema extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_process_id", nullable = false)
    private RecruitmentProcess recruitmentProcess;

    @Column(name = "version", nullable = false)
    private Long version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "application_form", columnDefinition = "jsonb")
    private String applicationForm;

    @Builder
    public RecruitmentSchema(RecruitmentProcess recruitmentProcess, Long version, String applicationForm) {
        this.recruitmentProcess = recruitmentProcess;
        this.version = version;
        this.applicationForm = applicationForm;
    }
}
