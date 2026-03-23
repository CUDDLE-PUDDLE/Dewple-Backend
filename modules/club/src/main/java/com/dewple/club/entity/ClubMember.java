package com.dewple.club.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityStatus;

import java.time.LocalDate;

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

    @Column(name = "activity_end_date")
    private LocalDate activityEndDate;


    @Builder
    public ClubMember(Club club, User user, ClubRole role, ClubGeneration joinGeneration,
                      ActivityStatus activityStatus, LocalDate activityEndDate) {
        this.club = club;
        this.user = user;
        this.role = role;
        this.joinGeneration = joinGeneration;
        this.activityStatus = activityStatus != null ? activityStatus : ActivityStatus.ACTIVE;
        this.activityEndDate = activityEndDate;
    }

    public void changeRole(ClubRole role) {
        this.role = role;
    }

    public void updateActivityStatus(ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
    }
}
