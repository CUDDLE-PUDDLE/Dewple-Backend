package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import com.dewple.entity.enums.ActivityStatus;

@Entity
@Table(name = "club_member"
//, uniqueConstraints = {@UniqueConstraint(columnNames = {"club_id", "user_id"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private ClubRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "join_generation_id")
    private ClubGeneration joinGeneration;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_status", nullable = false, length = 20)
    private ActivityStatus activityStatus = ActivityStatus.ACTIVE;

    
    @Builder
    public ClubMember(Club club, User user, ClubRole role, ClubGeneration joinGeneration,
                      ActivityStatus activityStatus) {
        this.club = club;
        this.user = user;
        this.role = role;
        this.joinGeneration = joinGeneration;
        this.activityStatus = activityStatus != null ? activityStatus : ActivityStatus.ACTIVE;
    }
}
