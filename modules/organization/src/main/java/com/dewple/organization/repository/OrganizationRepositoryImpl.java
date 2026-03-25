package com.dewple.organization.repository;

import com.dewple.common.enums.ActivityType;
import com.dewple.common.enums.ApprovalStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.OrganizationType;
import com.dewple.organization.entity.QOrganization;
import com.dewple.organization.service.GetOrganizationListParam;
import com.dewple.organization.service.OrganizationSummaryResult;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

@RequiredArgsConstructor
public class OrganizationRepositoryImpl implements OrganizationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QOrganization organization = QOrganization.organization;

    @Override
    public Slice<OrganizationSummaryResult> findOrganizationList(GetOrganizationListParam param) {
        Pageable pageable = param.pageable();

        List<OrganizationSummaryResult> content = queryFactory
                .select(Projections.constructor(OrganizationSummaryResult.class,
                        organization.id,
                        organization.name,
                        organization.coverImg,
                        organization.type,
                        organization.activityType,
                        organization.categoryIds,
                        organization.regionIds
                ))
                .from(organization)
                .where(
                        organization.approvalStatus.eq(ApprovalStatus.APPROVED),
                        organization.status.eq(BaseStatus.ACTIVE),
                        categoryIdContains(param.categoryId()),
                        regionIdContains(param.regionId()),
                        activityTypeEq(param.activityType()),
                        typeEq(param.type())
                )
                // TODO: 소속 회원수 순 정렬 (멤버십 기능 구현 후 변경)
                .orderBy(organization.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression categoryIdContains(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return Expressions.booleanTemplate(
                "jsonb_exists(cast({0} as jsonb), {1})",
                organization.categoryIds,
                String.valueOf(categoryId)
        );
    }

    private BooleanExpression regionIdContains(Long regionId) {
        if (regionId == null) {
            return null;
        }
        return Expressions.booleanTemplate(
                "jsonb_exists(cast({0} as jsonb), {1})",
                organization.regionIds,
                String.valueOf(regionId)
        );
    }

    private BooleanExpression activityTypeEq(ActivityType activityType) {
        return activityType != null ? organization.activityType.eq(activityType) : null;
    }

    private BooleanExpression typeEq(OrganizationType type) {
        return type != null ? organization.type.eq(type) : null;
    }
}
