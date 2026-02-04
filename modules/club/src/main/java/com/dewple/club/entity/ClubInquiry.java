package com.dewple.club.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;

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
    public ClubInquiry(Club club, User creator, ClubInquiry parent, String anonymityNickname, String content) {
        this.club = club;
        this.creator = creator;
        this.parent = parent;
        this.anonymityNickname = anonymityNickname;
        this.content = content;
    }
}
