package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_generation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberGeneration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private ClubMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private ClubGeneration generation;

    @Builder
    public MemberGeneration(ClubMember member, ClubGeneration generation) {
        this.member = member;
        this.generation = generation;
    }
}
