package com.dewple.activity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.dewple.common.entity.User;
import com.dewple.common.entity.BaseEntity;

@Entity
@Table(name = "activity_participant", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"activity_id", "participant_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @Column(name = "is_settlement_completed")
    private Boolean isSettlementCompleted;

    @Builder
    public ActivityParticipant(Activity activity, User participant, Boolean isSettlementCompleted) {
        this.activity = activity;
        this.participant = participant;
        this.isSettlementCompleted = isSettlementCompleted;
    }
}
