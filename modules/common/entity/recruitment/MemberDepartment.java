package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberDepartment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private ClubMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private ClubDepartment department;

    @Builder
    public MemberDepartment(ClubMember member, ClubDepartment department) {
        this.member = member;
        this.department = department;
    }
}
