package com.dewple.club.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;

@Entity
@Table(name = "vote_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private ClubMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_component_id", nullable = false)
    private VoteComponent voteComponent;

    @Builder
    public VoteParticipant(ClubMember member, VoteComponent voteComponent) {
        this.member = member;
        this.voteComponent = voteComponent;
    }
}
