package com.dewple.user.repository;

import com.dewple.user.entity.UserCategory;

import java.util.List;

public interface UserCategoryRepositoryCustom {

    List<UserCategory> findByUserIdWithCategory(Long userId);
}
