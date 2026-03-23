package com.dewple.activity.repository;

import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.entity.QActivity;
import com.dewple.activity.entity.QActivityParticipant;
import com.dewple.activity.service.ActivityHistoryResult;
import com.dewple.activity.service.ParticipantResult;
import com.dewple.common.enums.ActivityLifecycleStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ParticipantStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

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

    @Override
    public Slice<ParticipantResult> findParticipantListByActivityId(Long activityId, Pageable pageable) {
        List<ParticipantResult> content = queryFactory
                .select(Projections.constructor(ParticipantResult.class,
                        activityParticipant.id,
                        activityParticipant.participant.id,
                        activityParticipant.participant.profileImg,
                        activityParticipant.participant.name,
                        activityParticipant.participantStatus,
                        activityParticipant.createdAt
                ))
                .from(activityParticipant)
                .join(activityParticipant.participant)
                .where(
                        activityParticipant.activity.id.eq(activityId),
                        activityParticipant.status.eq(BaseStatus.ACTIVE)
                )
                .orderBy(activityParticipant.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<ActivityHistoryResult> findActivityHistoryByUserId(Long userId, Pageable pageable) {
        QActivity activity = QActivity.activity;

        List<ActivityHistoryResult> content = queryFactory
                .select(Projections.constructor(ActivityHistoryResult.class,
                        activity.id,
                        activity.name,
                        activity.thumbnailUrl,
                        Expressions.cases()
                                .when(activity.club.isNull().and(activity.organization.isNull()))
                                .then("PERSONAL")
                                .when(activity.club.isNotNull())
                                .then("CLUB")
                                .otherwise("ORGANIZATION"),
                        activity.club.name,
                        activity.startAt,
                        activity.endAt
                ))
                .from(activityParticipant)
                .join(activityParticipant.activity, activity)
                .leftJoin(activity.club)
                .where(
                        activityParticipant.participant.id.eq(userId),
                        activityParticipant.status.eq(BaseStatus.ACTIVE),
                        activityParticipant.participantStatus.eq(ParticipantStatus.CONFIRMED),
                        // 취소된 모임 제외
                        activity.lifecycleStatus.ne(ActivityLifecycleStatus.CANCELLED)
                )
                .orderBy(activity.endAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
