package com.dewple.activity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.BaseEntity;
import com.dewple.common.entity.User;

@Entity
@Table(name = "activity_interest", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"activity_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityInterest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public ActivityInterest(Activity activity, User user) {
        this.activity = activity;
        this.user = user;
    }
}
