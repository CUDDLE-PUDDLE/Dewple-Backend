package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "club_interest"
//, uniqueConstraints = { @UniqueConstraint(columnNames = {"club_id", "user_id"}) }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubInterest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public ClubInterest(Club club, User user) {
        this.club = club;
        this.user = user;
    }
}
