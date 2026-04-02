package com.dewple.recruitment.repository.custom;

import com.dewple.common.entity.QUser;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.entity.QApplication;
import com.dewple.recruitment.entity.QRecruitmentPosting;
import com.dewple.recruitment.entity.QRecruitmentProcess;
import com.dewple.recruitment.entity.QRecruitmentSchema;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ApplicationRepositoryImpl implements ApplicationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QApplication application = QApplication.application;
    private static final QRecruitmentSchema schema = QRecruitmentSchema.recruitmentSchema;
    private static final QRecruitmentProcess process = QRecruitmentProcess.recruitmentProcess;
    private static final QRecruitmentPosting posting = QRecruitmentPosting.recruitmentPosting;
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

    @Override
    public Optional<Application> findByPostingIdAndApplicantIdAndStatuses(
            Long postingId, Long applicantId, List<ApplicationStatus> statuses) {

        Application result = queryFactory
                .selectFrom(application)
                .join(application.recruitmentSchema, schema)
                .join(schema.recruitmentProcess, process)
                .where(
                        process.posting.id.eq(postingId),
                        application.applicant.id.eq(applicantId),
                        application.status.eq(BaseStatus.ACTIVE),
                        application.applicationStatus.in(statuses)
                )
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Application> findAllByApplicantIdWithPostingAndClub(Long applicantId) {
        return queryFactory
                .selectFrom(application)
                .join(application.recruitmentSchema, schema).fetchJoin()
                .join(schema.recruitmentProcess, process).fetchJoin()
                .join(process.posting, posting).fetchJoin()
                .join(posting.club).fetchJoin()
                .where(
                        application.applicant.id.eq(applicantId),
                        application.status.eq(BaseStatus.ACTIVE)
                )
                .orderBy(application.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<Application> findActiveByIdAndPostingId(Long applicationId, Long postingId) {
        Application result = queryFactory
                .selectFrom(application)
                .join(application.recruitmentSchema, schema)
                .join(schema.recruitmentProcess, process)
                .leftJoin(application.applicant, user).fetchJoin()
                .where(
                        application.id.eq(applicationId),
                        process.posting.id.eq(postingId),
                        application.status.eq(BaseStatus.ACTIVE)
                )
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Application> findActiveAllByIdsAndPostingId(List<Long> applicationIds, Long postingId) {
        return queryFactory
                .selectFrom(application)
                .join(application.recruitmentSchema, schema)
                .join(schema.recruitmentProcess, process)
                .where(
                        application.id.in(applicationIds),
                        process.posting.id.eq(postingId),
                        application.status.eq(BaseStatus.ACTIVE)
                )
                .fetch();
    }

    @Override
    public List<Application> findActiveByPostingIdAndStatus(Long postingId, ApplicationStatus status) {
        return queryFactory
                .selectFrom(application)
                .join(application.recruitmentSchema, schema)
                .join(schema.recruitmentProcess, process)
                .where(
                        process.posting.id.eq(postingId),
                        application.status.eq(BaseStatus.ACTIVE),
                        application.applicationStatus.eq(status)
                )
                .fetch();
    }

    @Override
    public long countByPostingIdAndStatus(Long postingId, ApplicationStatus status) {
        Long count = queryFactory
                .select(application.count())
                .from(application)
                .join(application.recruitmentSchema, schema)
                .join(schema.recruitmentProcess, process)
                .where(
                        process.posting.id.eq(postingId),
                        application.status.eq(BaseStatus.ACTIVE),
                        application.applicationStatus.eq(status)
                )
                .fetchOne();

        return count != null ? count : 0L;
    }
}
