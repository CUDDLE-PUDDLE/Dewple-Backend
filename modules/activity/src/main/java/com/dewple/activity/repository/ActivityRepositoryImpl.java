package com.dewple.activity.repository;

import com.dewple.activity.entity.QActivity;
import com.dewple.activity.entity.QActivityInterest;
import com.dewple.activity.entity.QActivityParticipant;
import com.dewple.activity.service.ActivitySummaryResult;
import com.dewple.club.entity.QClubInterest;
import com.dewple.club.entity.QClubMember;
import com.dewple.common.enums.BaseStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QActivity activity = QActivity.activity;
    private static final QActivityParticipant participant = QActivityParticipant.activityParticipant;
    private static final QActivityInterest activityInterest = QActivityInterest.activityInterest;
    private static final QClubInterest clubInterest = QClubInterest.clubInterest;
    private static final QClubMember clubMember = QClubMember.clubMember;

    @Override
    public Slice<ActivitySummaryResult> findPersonalActivities(Long userId, Pageable pageable) {
        return findActivities(userId, activity.club.isNull(), pageable);
    }

    @Override
    public Slice<ActivitySummaryResult> findActivitiesByLikedClubs(Long userId, Pageable pageable) {
        BooleanExpression condition = activity.club.id.in(
                JPAExpressions.select(clubInterest.club.id)
                        .from(clubInterest)
                        .where(
                                clubInterest.user.id.eq(userId),
                                clubInterest.status.eq(BaseStatus.ACTIVE)
                        )
        );
        return findActivities(userId, condition, pageable);
    }

    @Override
    public Slice<ActivitySummaryResult> findActivitiesByMyClubs(Long userId, Pageable pageable) {
        BooleanExpression condition = activity.club.id.in(
                JPAExpressions.select(clubMember.club.id)
                        .from(clubMember)
                        .where(
                                clubMember.user.id.eq(userId),
                                clubMember.status.eq(BaseStatus.ACTIVE)
                        )
        );
        return findActivities(userId, condition, pageable);
    }

    private Slice<ActivitySummaryResult> findActivities(Long userId, BooleanExpression sectionCondition, Pageable pageable) {
        QActivityParticipant subParticipant = new QActivityParticipant("subParticipant");

        List<ActivitySummaryResult> content = queryFactory
                .select(Projections.constructor(ActivitySummaryResult.class,
                        activity.id,
                        activity.thumbnailUrl,
                        Expressions.cases()
                                .when(activity.club.isNull()).then("PERSONAL")
                                .otherwise("CLUB"),
                        activity.club.name,
                        activity.name,
                        activity.category.name,
                        activity.region.name,
                        JPAExpressions.select(subParticipant.count().intValue())
                                .from(subParticipant)
                                .where(
                                        subParticipant.activity.id.eq(activity.id),
                                        subParticipant.status.eq(BaseStatus.ACTIVE)
                                ),
                        activity.capacity,
                        activity.likeCount,
                        activity.viewCount,
                        activity.commentCount,
                        JPAExpressions.selectOne()
                                .from(activityInterest)
                                .where(
                                        activityInterest.activity.id.eq(activity.id),
                                        activityInterest.user.id.eq(userId),
                                        activityInterest.status.eq(BaseStatus.ACTIVE)
                                )
                                .exists()
                ))
                .from(activity)
                .leftJoin(activity.club)
                .leftJoin(activity.category)
                .leftJoin(activity.region)
                .where(
                        sectionCondition,
                        activity.status.eq(BaseStatus.ACTIVE)
                )
                .orderBy(activity.createdAt.desc())
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
