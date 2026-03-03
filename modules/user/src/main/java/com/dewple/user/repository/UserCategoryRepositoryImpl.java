package com.dewple.user.repository;

import com.dewple.user.entity.QUserCategory;
import com.dewple.user.entity.UserCategory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class UserCategoryRepositoryImpl implements UserCategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QUserCategory userCategory = QUserCategory.userCategory;

    @Override
    public List<UserCategory> findByUserIdWithCategory(Long userId) {
        return queryFactory
                .selectFrom(userCategory)
                .join(userCategory.category).fetchJoin()
                .where(userCategory.user.id.eq(userId))
                .fetch();
    }
}
