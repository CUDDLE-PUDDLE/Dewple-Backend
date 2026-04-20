package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.entity.StarRating;
import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.activity.repository.StarRatingRepository;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityLifecycleStatus;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ParticipantRole;
import com.dewple.common.enums.ParticipantStatus;
import com.dewple.common.exception.BusinessException;
import com.dewple.common.exception.CommonErrorCode;
import com.dewple.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarRatingService {

    private static final int RATING_PERIOD_DAYS = 7;

    private final ActivityRepository activityRepository;
    private final ActivityParticipantRepository participantRepository;
    private final StarRatingRepository starRatingRepository;
    private final UserRepository userRepository;

    @Transactional
    public void rateParticipant(Long raterId, Long activityId, Long rateeId, BigDecimal score) {
        if (raterId.equals(rateeId)) {
            throw new BusinessException(ActivityErrorCode.CANNOT_RATE_SELF);
        }

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        validateRatingPeriod(activity);
        validateConfirmedParticipant(activityId, raterId);
        validateConfirmedParticipant(activityId, rateeId);

        if (starRatingRepository.existsByActivityIdAndRaterIdAndRateeIdAndStatus(
                activityId, raterId, rateeId, BaseStatus.ACTIVE)) {
            throw new BusinessException(ActivityErrorCode.ALREADY_RATED);
        }

        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new BusinessException(ActivityErrorCode.INVALID_RATING_SCORE);
        }

        // 0.1단위 검증 (소수점 둘째자리 이하 불가)
        if (score.stripTrailingZeros().scale() > 1) {
            throw new BusinessException(ActivityErrorCode.INVALID_RATING_SCORE);
        }

        User rater = userRepository.findById(raterId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
        User ratee = userRepository.findById(rateeId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));

        // No-show 여부 확인
        boolean isNoShow = participantRepository.findByActivityIdAndParticipantId(activityId, rateeId)
                .map(ActivityParticipant::getParticipantStatus)
                .map(s -> s == ParticipantStatus.NO_SHOW)
                .orElse(false);

        StarRating rating = StarRating.builder()
                .activity(activity)
                .rater(rater)
                .ratee(ratee)
                .score(score)
                .isNoShow(isNoShow)
                .build();

        starRatingRepository.save(rating);
        updateReputationScore(rateeId);

        log.info("별점 평가: raterId={}, rateeId={}, activityId={}, score={}", raterId, rateeId, activityId, score);
    }

    @Transactional(readOnly = true)
    public List<RatingTargetResult> getRatingTargets(Long userId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        validateRatingPeriod(activity);

        List<ActivityParticipant> allConfirmed = participantRepository
                .findByActivityIdAndStatusAndParticipantStatusAndRole(
                        activityId, BaseStatus.ACTIVE, ParticipantStatus.CONFIRMED, ParticipantRole.PARTICIPANT);

        // LEADER/MANAGER도 포함
        allConfirmed.addAll(participantRepository
                .findByActivityIdAndStatusAndParticipantStatusAndRole(
                        activityId, BaseStatus.ACTIVE, ParticipantStatus.CONFIRMED, ParticipantRole.LEADER));
        allConfirmed.addAll(participantRepository
                .findByActivityIdAndStatusAndParticipantStatusAndRole(
                        activityId, BaseStatus.ACTIVE, ParticipantStatus.CONFIRMED, ParticipantRole.MANAGER));

        List<StarRating> myRatings = starRatingRepository.findByActivityIdAndRaterIdAndStatus(
                activityId, userId, BaseStatus.ACTIVE);

        List<Long> alreadyRatedIds = myRatings.stream()
                .map(r -> r.getRatee().getId())
                .toList();

        return allConfirmed.stream()
                .filter(p -> !p.getParticipant().getId().equals(userId))
                .map(p -> new RatingTargetResult(
                        p.getParticipant().getId(),
                        p.getParticipant().getName(),
                        p.getParticipant().getProfileImg(),
                        alreadyRatedIds.contains(p.getParticipant().getId()),
                        p.getParticipantStatus() == ParticipantStatus.NO_SHOW
                ))
                .toList();
    }

    @Transactional
    public void markNoShow(Long userId, Long activityId, Long participantId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (activity.getLifecycleStatus() != ActivityLifecycleStatus.ENDED) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }

        boolean isLeaderOrManager = participantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .map(p -> p.getRole() == ParticipantRole.LEADER || p.getRole() == ParticipantRole.MANAGER)
                .orElse(false);

        if (!isLeaderOrManager) {
            throw new BusinessException(ActivityErrorCode.NOT_ACTIVITY_LEADER);
        }

        ActivityParticipant participant = participantRepository
                .findByActivityIdAndParticipantId(activityId, participantId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_FOUND));

        participant.updateParticipantStatus(ParticipantStatus.NO_SHOW);
        log.info("No-show 지정: participantId={}, activityId={}", participantId, activityId);
    }

    private void validateRatingPeriod(Activity activity) {
        if (activity.getLifecycleStatus() != ActivityLifecycleStatus.ENDED) {
            throw new BusinessException(ActivityErrorCode.RATING_NOT_AVAILABLE);
        }
        if (activity.getEndAt().plusDays(RATING_PERIOD_DAYS).isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ActivityErrorCode.RATING_PERIOD_EXPIRED);
        }
    }

    private void validateConfirmedParticipant(Long activityId, Long userId) {
        participantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .filter(p -> p.getParticipantStatus() == ParticipantStatus.CONFIRMED
                        || p.getParticipantStatus() == ParticipantStatus.NO_SHOW)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_CONFIRMED));
    }

    private void updateReputationScore(Long userId) {
        starRatingRepository.findAverageScoreByRateeId(userId, BaseStatus.ACTIVE)
                .ifPresent(avg -> {
                    // 소수점 1자리, 올림
                    BigDecimal rounded = avg.setScale(1, RoundingMode.CEILING);
                    User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
                    user.updateReputationScore(rounded);
                });
    }

    public record RatingTargetResult(
            Long userId, String name, String profileImg,
            boolean alreadyRated, boolean isNoShow
    ) {}
}
