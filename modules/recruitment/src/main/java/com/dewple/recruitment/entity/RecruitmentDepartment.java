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

    @Column(name = "count", nullable = false)
    private Integer count;

    @Builder
    public RecruitmentDepartment(RecruitmentPosting recruitment, ClubDepartment department, Integer count) {
        this.recruitment = recruitment;
        this.department = department;
        this.count = count;
    }

    // 연관관계 편의 메소드
    void changeRecruitment(RecruitmentPosting recruitment) {
        this.recruitment = recruitment;
    }
}
