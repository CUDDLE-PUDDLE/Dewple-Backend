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
@Table(name = "club_deletion_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubDeletionVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Builder
    public ClubDeletionVote(Club club, User user, Boolean isApproved) {
        this.club = club;
        this.user = user;
        this.isApproved = isApproved;
    }

    public void approve() {
        this.isApproved = true;
    }

    public void reject() {
        this.isApproved = false;
    }
}
