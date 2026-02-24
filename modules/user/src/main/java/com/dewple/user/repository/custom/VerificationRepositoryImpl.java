package com.dewple.user.repository.custom;

import com.dewple.common.enums.VerificationType;
import com.dewple.user.entity.QVerification;
import com.dewple.user.entity.Verification;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class VerificationRepositoryImpl implements VerificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QVerification verification = QVerification.verification;

    @Override
    public Optional<Verification> findLatestByDataAndType(String data, VerificationType type) {
        Verification result = queryFactory
                .selectFrom(verification)
                .where(
                        verification.target.eq(data),
                        verification.type.eq(type)
                )
                .orderBy(verification.createdAt.desc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }
}
