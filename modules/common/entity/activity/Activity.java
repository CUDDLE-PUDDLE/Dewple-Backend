package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dewple.entity.enums.OpenType;

@Entity
@Table(name = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "open_type", nullable = false, length = 30)
    private OpenType openType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime endAt;

    @Builder
    public Activity(Club club, User creator, OpenType openType, String name,
                    OffsetDateTime startAt, OffsetDateTime endAt) {
        this.club = club;
        this.creator = creator;
        this.openType = openType;
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
