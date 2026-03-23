package com.dewple.activity.repository;

import com.dewple.activity.entity.StarRating;
import com.dewple.common.enums.BaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StarRatingRepository extends JpaRepository<StarRating, Long> {

    boolean existsByActivityIdAndRaterIdAndRateeIdAndStatus(
            Long activityId, Long raterId, Long rateeId, BaseStatus status);

    List<StarRating> findByActivityIdAndRaterIdAndStatus(
            Long activityId, Long raterId, BaseStatus status);

    @Query("SELECT AVG(sr.score) FROM StarRating sr WHERE sr.ratee.id = :rateeId AND sr.status = :status")
    Optional<BigDecimal> findAverageScoreByRateeId(@Param("rateeId") Long rateeId, @Param("status") BaseStatus status);

    long countByRateeIdAndStatus(Long rateeId, BaseStatus status);
}
