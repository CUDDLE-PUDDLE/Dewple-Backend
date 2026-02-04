package com.dewple.club.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;

@Entity
@Table(name = "vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous;

    @Column(name = "is_multi", nullable = false)
    private Boolean isMulti;

    @Column(name = "is_modifiable", nullable = false)
    private Boolean isModifiable;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime endAt;


    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteComponent> voteComponents = new ArrayList<>();

    
    @Builder
    public Vote(Notice notice, User creator, String name, String description,
                Boolean isAnonymous, Boolean isMulti, Boolean isModifiable,
                OffsetDateTime startAt, OffsetDateTime endAt) {
        this.notice = notice;
        this.creator = creator;
        this.name = name;
        this.description = description;
        this.isAnonymous = isAnonymous;
        this.isMulti = isMulti;
        this.isModifiable = isModifiable;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
