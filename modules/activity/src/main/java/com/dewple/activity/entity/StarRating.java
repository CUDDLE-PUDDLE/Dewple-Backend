package com.dewple.activity.entity;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "star_rating", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"activity_id", "rater_id", "ratee_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StarRating extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private User rater;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ratee_id", nullable = false)
    private User ratee;

    @Column(name = "score", nullable = false, precision = 2, scale = 1)
    private BigDecimal score;

    @Column(name = "is_no_show", nullable = false)
    private Boolean isNoShow = false;

    @Builder
    public StarRating(Activity activity, User rater, User ratee, BigDecimal score, Boolean isNoShow) {
        this.activity = activity;
        this.rater = rater;
        this.ratee = ratee;
        this.score = score;
        this.isNoShow = isNoShow != null ? isNoShow : false;
    }
}
