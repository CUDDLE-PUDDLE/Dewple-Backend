package com.dewple.activity.repository;

import com.dewple.common.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("activityCategoryRepository")
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
