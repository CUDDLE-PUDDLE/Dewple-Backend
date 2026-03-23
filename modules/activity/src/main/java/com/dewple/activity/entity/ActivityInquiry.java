package com.dewple.activity.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityInquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_id")
    private User answeredBy;

    @Builder
    public ActivityInquiry(Activity activity, User author, String content, Boolean isAnonymous) {
        this.activity = activity;
        this.author = author;
        this.content = content;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
    }

    public boolean hasAnswer() {
        return this.answer != null;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void setAnswer(String answer, User answeredBy) {
        this.answer = answer;
        this.answeredBy = answeredBy;
    }

    public void updateAnswer(String answer) {
        this.answer = answer;
    }
}
