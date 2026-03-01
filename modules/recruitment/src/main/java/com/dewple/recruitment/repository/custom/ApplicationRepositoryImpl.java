package com.dewple.recruitment.repository.custom;

import com.dewple.common.entity.QUser;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.entity.QApplication;
import com.dewple.recruitment.entity.QRecruitmentProcess;
import com.dewple.recruitment.entity.QRecruitmentSchema;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class ApplicationRepositoryImpl implements ApplicationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QApplication application = QApplication.application;
    private static final QRecruitmentSchema schema = QRecruitmentSchema.recruitmentSchema;
    private static final QRecruitmentProcess process = QRecruitmentProcess.recruitmentProcess;
    private static final QUser user = QUser.user;

    @Override
    public Page<Application> searchByPostingId(Long postingId, ApplicationStatus status,
                                                String keyword, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(process.posting.id.eq(postingId));
        where.and(application.status.eq(BaseStatus.ACTIVE));

        if (status != null) {
            where.and(application.applicationStatus.eq(status));
        }

        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            where.and(
                    user.name.containsIgnoreCase(trimmed)
                            .or(user.phone.contains(trimmed))
                            .or(application.guestPhone.contains(trimmed))
            );
        }

        Long total = queryFactory
                .select(application.count())
                .from(application)
                .join(application.recruitmentSchema, schema)
                .join(schema.recruitmentProcess, process)
                .leftJoin(application.applicant, user)
                .where(where)
                .fetchOne();

        List<Application> content = queryFactory
                .selectFrom(application)
                .join(application.recruitmentSchema, schema)
                .join(schema.recruitmentProcess, process)
                .leftJoin(application.applicant, user).fetchJoin()
                .where(where)
                .orderBy(application.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
