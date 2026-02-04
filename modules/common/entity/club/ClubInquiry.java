package com.dewple.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "club_inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubInquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ClubInquiry parent;

    @Column(name = "anonymity_nickname", nullable = false, length = 50)
    private String anonymityNickname;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;


    @Builder
    public ClubInquiry(Club club, User creator, ClubInquiry parent, String anonymity, String content) {
        this.club = club;
        this.creator = creator;
        this.parent = parent;
        this.anonymity = anonymity;
        this.content = content;
    }
}
