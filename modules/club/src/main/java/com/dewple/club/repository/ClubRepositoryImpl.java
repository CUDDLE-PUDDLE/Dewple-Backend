package com.dewple.club.repository;

import com.dewple.club.entity.QClubCategory;
import com.dewple.club.entity.QClubMember;
import com.dewple.club.entity.QClubRegion;
import com.dewple.club.service.ClubSummaryResult;
import com.dewple.club.service.GetClubListParam;
import com.dewple.common.entity.QClub;
import com.dewple.common.enums.ActivityStatus;
import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.BaseStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

@RequiredArgsConstructor
public class ClubRepositoryImpl implements ClubRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QClub club = QClub.club;
    private static final QClubMember clubMember = QClubMember.clubMember;
    private static final QClubCategory clubCategory = QClubCategory.clubCategory;
    private static final QClubRegion clubRegion = QClubRegion.clubRegion;

    @Override
    public Slice<ClubSummaryResult> findClubList(GetClubListParam param) {
        Pageable pageable = param.pageable();

        NumberExpression<Long> memberCountExpr = clubMember.id.count();

        List<ClubSummaryResult> content = queryFactory
                .select(Projections.constructor(ClubSummaryResult.class,
                        club.id,
                        club.name,
                        club.coverImg,
                        club.activityType,
                        club.isVerificationRequired,
                        club.likeCount,
                        memberCountExpr
                ))
                .from(club)
                .leftJoin(clubMember)
                    .on(clubMember.club.id.eq(club.id)
                            .and(clubMember.status.eq(BaseStatus.ACTIVE))
                            .and(clubMember.activityStatus.eq(ActivityStatus.ACTIVE)))
                .where(
                        club.status.eq(BaseStatus.ACTIVE),
                        isVerificationRequiredEq(param.isVerificationRequired()),
                        activityTypeEq(param.activityType()),
                        categoryIdEq(param.categoryId()),
                        regionIdEq(param.regionId())
                )
                .groupBy(club.id)
                .orderBy(memberCountExpr.add(club.likeCount).desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression isVerificationRequiredEq(Boolean isVerificationRequired) {
        return isVerificationRequired != null ? club.isVerificationRequired.eq(isVerificationRequired) : null;
    }

    private BooleanExpression activityTypeEq(ActivityType activityType) {
        return activityType != null ? club.activityType.eq(activityType) : null;
    }

    private BooleanExpression categoryIdEq(Long categoryId) {
        if (categoryId == null) return null;
        return club.id.in(
                queryFactory.select(clubCategory.club.id)
                        .from(clubCategory)
                        .where(clubCategory.category.id.eq(categoryId))
        );
    }

    private BooleanExpression regionIdEq(Long regionId) {
        if (regionId == null) return null;
        return club.id.in(
                queryFactory.select(clubRegion.club.id)
                        .from(clubRegion)
                        .where(clubRegion.region.id.eq(regionId))
        );
    }
}
