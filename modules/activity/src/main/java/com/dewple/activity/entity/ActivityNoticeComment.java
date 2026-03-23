package com.dewple.activity.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_notice_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityNoticeComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private ActivityNotice notice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public ActivityNoticeComment(ActivityNotice notice, User author, String content) {
        this.notice = notice;
        this.author = author;
        this.content = content;
    }
}
