package com.dewple.activity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.OpenType;

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

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "is_attendance_check", nullable = false)
    private Boolean isAttendanceCheck = false;

    @Column(name = "is_searchable", nullable = false)
    private Boolean isSearchable = true;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime endAt;

    @Builder
    public Activity(Club club, User creator, OpenType openType, String name,
                    String description, Integer capacity, Boolean isAttendanceCheck,
                    Boolean isSearchable, OffsetDateTime startAt, OffsetDateTime endAt) {
        this.club = club;
        this.creator = creator;
        this.openType = openType;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.isAttendanceCheck = isAttendanceCheck != null ? isAttendanceCheck : false;
        this.isSearchable = isSearchable != null ? isSearchable : true;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
