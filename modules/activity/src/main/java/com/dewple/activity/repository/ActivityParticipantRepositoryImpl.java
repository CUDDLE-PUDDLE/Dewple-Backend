package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.entity.QActivityParticipant;
import com.dewple.common.enums.BaseStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityParticipantRepositoryImpl implements ActivityParticipantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QActivityParticipant activityParticipant = QActivityParticipant.activityParticipant;

    @Override
    public List<ActivityParticipant> findByActivityIdAndStatusWithParticipant(Long activityId, BaseStatus status) {
        return queryFactory
                .selectFrom(activityParticipant)
                .join(activityParticipant.participant).fetchJoin()
                .where(
                        activityParticipant.activity.id.eq(activityId),
                        activityParticipant.status.eq(status)
                )
                .fetch();
    }
}
