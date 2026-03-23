package com.dewple.activity.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "activity_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    private String imageUrls;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Builder
    public ActivityNotice(Activity activity, User author, String title, String content, String imageUrls) {
        this.activity = activity;
        this.author = author;
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
    }

    public void update(String title, String content, String imageUrls) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (imageUrls != null) this.imageUrls = imageUrls;
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }
}
