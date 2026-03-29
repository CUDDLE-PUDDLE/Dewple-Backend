package com.dewple.club.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club_kick_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubKickVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_member_id", nullable = false)
    private ClubMember targetMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Builder
    public ClubKickVote(Club club, ClubMember targetMember, User voter, Boolean isApproved) {
        this.club = club;
        this.targetMember = targetMember;
        this.voter = voter;
        this.isApproved = isApproved;
    }

    public void approve() {
        this.isApproved = true;
    }
}
