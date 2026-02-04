package com.dewple.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;
import com.dewple.club.entity.ClubDepartment;

@Entity
@Table(name = "recruitment_department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentDepartment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private RecruitmentPosting recruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private ClubDepartment department;

    @Builder
    public RecruitmentDepartment(RecruitmentPosting recruitment, ClubDepartment department) {
        this.recruitment = recruitment;
        this.department = department;
    }
}
