package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityInterest;
import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityInterestRepository;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.activity.repository.CategoryRepository;
import com.dewple.activity.repository.RegionRepository;
import com.dewple.club.entity.ClubMember;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.club.repository.ClubRepository;
import com.dewple.common.entity.Category;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.Region;
import com.dewple.common.entity.User;
import com.dewple.common.enums.ActivityLifecycleStatus;
import com.dewple.common.enums.ParticipantRole;
import com.dewple.common.enums.ParticipantStatus;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.OpenType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityInterestRepository activityInterestRepository;
    private final ActivityParticipantRepository activityParticipantRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;

    private static final int MAX_LEADER_ACTIVITIES = 10;

    @Transactional
    public CreateActivityResult createActivity(Long userId, CreateActivityParam param) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // #25: 모임장 동시 운영 최대 10개
        long leaderCount = activityRepository.findByCreatorIdAndStatus(userId, BaseStatus.ACTIVE).size();
        if (leaderCount >= MAX_LEADER_ACTIVITIES) {
            throw new BusinessException(ActivityErrorCode.LEADER_ACTIVITY_LIMIT_EXCEEDED);
        }

        if (param.endAt().isBefore(param.startAt()) || param.endAt().isEqual(param.startAt())) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_END_BEFORE_START);
        }

        if (param.startAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_START_IN_PAST);
        }

        // #23: capacity 0명이면 PRIVATE 자동 전환
        OpenType resolvedOpenType = param.openType();
        if (param.capacity() != null && param.capacity() == 0) {
            resolvedOpenType = OpenType.PRIVATE;
        }

        Club club = null;
        if (param.clubId() != null) {
            club = clubRepository.findById(param.clubId())
                    .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));

            ClubMember member = clubMemberRepository.findByClubIdAndUserId(param.clubId(), userId)
                    .orElseThrow(() -> new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER));

            if (member.getRole() == null || !member.getRole().hasPermission(Permission.MANAGE_ACTIVITY)) {
                throw new BusinessException(ClubErrorCode.CLUB_PERMISSION_DENIED);
            }
        }

        Category category = null;
        if (param.categoryId() != null) {
            category = categoryRepository.findById(param.categoryId())
                    .orElseThrow(() -> new BusinessException(ActivityErrorCode.CATEGORY_NOT_FOUND));
        }

        Region region = null;
        if (param.regionId() != null) {
            region = regionRepository.findById(param.regionId())
                    .orElseThrow(() -> new BusinessException(ActivityErrorCode.REGION_NOT_FOUND));
        }

        String inviteCode = null;
        if (resolvedOpenType == OpenType.PRIVATE && param.clubId() == null) {
            inviteCode = UUID.randomUUID().toString();
        }

        String managerInviteCode = UUID.randomUUID().toString();

        Activity activity = Activity.builder()
                .club(club)
                .creator(creator)
                .openType(resolvedOpenType)
                .name(param.name())
                .description(param.description())
                .capacity(param.capacity())
                .isAttendanceCheck(param.isAttendanceCheck())
                .isSearchable(param.isSearchable())
                .startAt(param.startAt())
                .endAt(param.endAt())
                .category(category)
                .region(region)
                .activityType(param.activityType())
                .isVerificationRequired(param.isVerificationRequired())
                .minAge(param.minAge())
                .maxAge(param.maxAge())
                .gender(param.gender())
                .inviteCode(inviteCode)
                .managerInviteCode(managerInviteCode)
                .emergencyContact(param.emergencyContact())
                .cancelDeadlineDays(param.cancelDeadlineDays())
                .build();

        activityRepository.save(activity);

        // 모임장을 LEADER로 참여자 등록 (capacity에 미포함, 별도 카운트)
        ActivityParticipant leaderParticipant = ActivityParticipant.builder()
                .activity(activity)
                .participant(creator)
                .participantStatus(ParticipantStatus.CONFIRMED)
                .role(ParticipantRole.LEADER)
                .build();
        activityParticipantRepository.save(leaderParticipant);

        log.info("모임 생성 완료: activityId={}, creatorId={}", activity.getId(), userId);

        return new CreateActivityResult(
                activity.getId(),
                club != null ? club.getId() : null,
                club != null ? club.getName() : null,
                activity.getOpenType(),
                activity.getName(),
                activity.getDescription(),
                activity.getCapacity(),
                activity.getIsAttendanceCheck(),
                activity.getIsSearchable(),
                activity.getStartAt(),
                activity.getEndAt(),
                activity.getCreatedAt(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                region != null ? region.getId() : null,
                region != null ? region.getName() : null,
                activity.getActivityType(),
                activity.getIsVerificationRequired(),
                activity.getMinAge(),
                activity.getMaxAge(),
                activity.getGender()
        );
    }

    @Transactional
    public void deleteActivity(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);

        boolean isCreator = activity.getCreator().getId().equals(userId);

        if (activity.getClub() != null) {
            if (!isCreator) {
                ClubMember member = clubMemberRepository.findByClubIdAndUserId(activity.getClub().getId(), userId)
                        .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED));

                if (member.getRole() == null || !member.getRole().hasPermission(Permission.MANAGE_ACTIVITY)) {
                    throw new BusinessException(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED);
                }
            }
        } else {
            if (!isCreator) {
                throw new BusinessException(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED);
            }
        }

        activity.cancel();
        log.info("모임 삭제(취소) 완료: activityId={}, userId={}", activityId, userId);
    }

    @Transactional(readOnly = true)
    public GetActivityDetailResult getActivityDetail(Long activityId) {
        Activity activity = findActiveActivity(activityId);

        List<ActivityParticipant> participants = activityParticipantRepository
                .findByActivityIdAndStatusWithParticipant(activityId, BaseStatus.ACTIVE);

        List<GetActivityDetailResult.ParticipantInfo> participantInfos = participants.stream()
                .map(p -> new GetActivityDetailResult.ParticipantInfo(
                        p.getParticipant().getId(),
                        p.getParticipant().getProfileImg(),
                        p.getParticipant().getName()
                ))
                .toList();

        Club club = activity.getClub();

        return new GetActivityDetailResult(
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                club != null ? club.getId() : null,
                club != null ? club.getName() : null,
                activity.getOpenType(),
                activity.getCapacity(),
                activity.getStartAt(),
                activity.getEndAt(),
                participantInfos
        );
    }

    @Transactional(readOnly = true)
    public Slice<ActivitySummaryResult> getActivityList(Long userId, GetActivityListParam param) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        return switch (param.section()) {
            case PERSONAL -> activityRepository.findPersonalActivities(userId, param.pageable());
            case LIKED_CLUBS -> activityRepository.findActivitiesByLikedClubs(userId, param.pageable());
            case MY_CLUBS -> activityRepository.findActivitiesByMyClubs(userId, param.pageable());
        };
    }

    @Transactional(readOnly = true)
    public Slice<ParticipantResult> getParticipantList(Long userId, Long activityId, Pageable pageable) {
        Activity activity = findActiveActivity(activityId);

        boolean isCreator = activity.getCreator().getId().equals(userId);

        if (activity.getClub() != null) {
            if (!isCreator) {
                ClubMember member = clubMemberRepository.findByClubIdAndUserId(activity.getClub().getId(), userId)
                        .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_VIEW_PERMISSION_DENIED));

                if (member.getRole() == null || !member.getRole().hasPermission(Permission.MANAGE_ACTIVITY)) {
                    throw new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_VIEW_PERMISSION_DENIED);
                }
            }
        } else {
            if (!isCreator) {
                throw new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_VIEW_PERMISSION_DENIED);
            }
        }

        return activityParticipantRepository.findParticipantListByActivityId(activityId, pageable);
    }

    @Transactional
    public void updateParticipantStatus(Long userId, Long activityId, Long participantId, ParticipantStatus status) {
        if (status != ParticipantStatus.APPROVED && status != ParticipantStatus.REJECTED) {
            throw new BusinessException(ActivityErrorCode.INVALID_PARTICIPANT_STATUS);
        }

        Activity activity = findActiveActivity(activityId);

        boolean isCreator = activity.getCreator().getId().equals(userId);

        if (activity.getClub() != null) {
            if (!isCreator) {
                ClubMember member = clubMemberRepository.findByClubIdAndUserId(activity.getClub().getId(), userId)
                        .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_MANAGE_PERMISSION_DENIED));

                if (member.getRole() == null || !member.getRole().hasPermission(Permission.MANAGE_ACTIVITY)) {
                    throw new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_MANAGE_PERMISSION_DENIED);
                }
            }
        } else {
            if (!isCreator) {
                throw new BusinessException(ActivityErrorCode.ACTIVITY_PARTICIPANT_MANAGE_PERMISSION_DENIED);
            }
        }

        ActivityParticipant participant = activityParticipantRepository.findById(participantId)
                .filter(p -> p.getActivity().getId().equals(activityId))
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_FOUND));

        participant.updateParticipantStatus(status);
        log.info("지원자 상태 변경: participantId={}, activityId={}, status={}", participantId, activityId, status);
    }

    @Transactional
    public void respondToParticipation(Long userId, Long activityId, ParticipantStatus status) {
        if (status != ParticipantStatus.CONFIRMED && status != ParticipantStatus.DECLINED) {
            throw new BusinessException(ActivityErrorCode.INVALID_PARTICIPATION_RESPONSE);
        }

        Activity activity = findActiveActivity(activityId);

        ActivityParticipant participant = activityParticipantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_FOUND));

        if (participant.getParticipantStatus() != ParticipantStatus.APPROVED) {
            throw new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_APPROVED);
        }

        participant.updateParticipantStatus(status);
        log.info("모임 참여 응답: userId={}, activityId={}, status={}", userId, activityId, status);
    }

    @Transactional
    public void cancelParticipation(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);

        // #18: 모임 시작 후 취소 불가
        if (activity.getStartAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_ALREADY_STARTED);
        }

        // #18: cancelDeadlineDays 기반 기한 체크
        OffsetDateTime cancelDeadline = activity.getStartAt()
                .minusDays(activity.getCancelDeadlineDays());
        if (OffsetDateTime.now().isAfter(cancelDeadline)) {
            throw new BusinessException(ActivityErrorCode.CANCEL_DEADLINE_EXCEEDED);
        }

        ActivityParticipant participant = activityParticipantRepository
                .findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_FOUND));

        if (participant.getParticipantStatus() != ParticipantStatus.CONFIRMED
                && participant.getParticipantStatus() != ParticipantStatus.APPROVED) {
            throw new BusinessException(ActivityErrorCode.PARTICIPANT_NOT_CONFIRMED);
        }

        participant.updateParticipantStatus(ParticipantStatus.CANCELLED);

        if (!activity.getHasApplicationForm()) {
            promoteFromWaitlist(activity);
        }

        log.info("참여 취소: userId={}, activityId={}", userId, activityId);
    }

    // ========== 선착순 대기열 ==========

    @Transactional
    public void applyFirstCome(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);

        if (activity.getHasApplicationForm()) {
            throw new BusinessException(ActivityErrorCode.NOT_FIRST_COME_ACTIVITY);
        }
        if (activity.getStartAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_ALREADY_STARTED);
        }
        if (activity.getCreator().getId().equals(userId)) {
            throw new BusinessException(ActivityErrorCode.CANNOT_JOIN_OWN_ACTIVITY);
        }

        activityParticipantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .ifPresent(p -> { throw new BusinessException(ActivityErrorCode.ALREADY_PARTICIPANT); });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        long confirmedCount = activityParticipantRepository
                .countByActivityIdAndStatusAndParticipantStatusInAndRole(
                        activityId, BaseStatus.ACTIVE,
                        List.of(ParticipantStatus.CONFIRMED), ParticipantRole.PARTICIPANT);

        boolean withinCapacity = activity.getCapacity() == null || confirmedCount < activity.getCapacity();

        if (withinCapacity) {
            activityParticipantRepository.save(ActivityParticipant.builder()
                    .activity(activity).participant(user)
                    .participantStatus(ParticipantStatus.CONFIRMED)
                    .role(ParticipantRole.PARTICIPANT).build());
            log.info("선착순 즉시 확정: userId={}, activityId={}", userId, activityId);
        } else {
            int nextOrder = activityParticipantRepository
                    .findFirstByActivityIdAndStatusAndRoleOrderByWaitlistOrderDesc(
                            activityId, BaseStatus.ACTIVE, ParticipantRole.PARTICIPANT)
                    .map(p -> p.getWaitlistOrder() != null ? p.getWaitlistOrder() + 1 : 1)
                    .orElse(1);

            activityParticipantRepository.save(ActivityParticipant.builder()
                    .activity(activity).participant(user)
                    .participantStatus(ParticipantStatus.PENDING)
                    .role(ParticipantRole.PARTICIPANT).waitlistOrder(nextOrder).build());
            log.info("선착순 대기열 등록: userId={}, activityId={}, order={}", userId, activityId, nextOrder);
        }
    }

    @Transactional
    public void cancelWaitlist(Long userId, Long activityId) {
        findActiveActivity(activityId);

        ActivityParticipant participant = activityParticipantRepository
                .findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .filter(p -> p.getParticipantStatus() == ParticipantStatus.PENDING)
                .filter(p -> p.getWaitlistOrder() != null)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.NOT_IN_WAITLIST));

        participant.updateParticipantStatus(ParticipantStatus.CANCELLED);
        log.info("대기열 취소: userId={}, activityId={}", userId, activityId);
    }

    @Transactional
    public void selectFromWaitlist(Long userId, Long activityId, Long participantId) {
        Activity activity = findActiveActivity(activityId);

        if (!isLeaderOrManager(activityId, userId)) {
            throw new BusinessException(ActivityErrorCode.NOT_ACTIVITY_LEADER);
        }
        if (!activity.getIsVerificationRequired()) {
            throw new BusinessException(ActivityErrorCode.VERIFICATION_REQUIRED_FOR_SELECT);
        }

        ActivityParticipant participant = activityParticipantRepository.findById(participantId)
                .filter(p -> p.getActivity().getId().equals(activityId))
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .filter(p -> p.getParticipantStatus() == ParticipantStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.NOT_IN_WAITLIST));

        participant.updateParticipantStatus(ParticipantStatus.CONFIRMED);
        participant.clearWaitlistOrder();
        log.info("대기자 선택 참여: participantId={}, activityId={}", participantId, activityId);
    }

    private void promoteFromWaitlist(Activity activity) {
        activityParticipantRepository
                .findFirstByActivityIdAndStatusAndParticipantStatusAndRoleOrderByWaitlistOrderAsc(
                        activity.getId(), BaseStatus.ACTIVE,
                        ParticipantStatus.PENDING, ParticipantRole.PARTICIPANT)
                .ifPresent(next -> {
                    next.updateParticipantStatus(ParticipantStatus.CONFIRMED);
                    next.clearWaitlistOrder();
                    // TODO: 승격된 대기자에게 알림 (푸시+알림톡)
                    log.info("대기자 자동 승격: userId={}, activityId={}",
                            next.getParticipant().getId(), activity.getId());
                });
    }

    // ========== 모임 취소 ==========

    @Transactional
    public void cancelActivityManually(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);
        validateLeader(activity, userId);

        cancelActivity(activity);
        // TODO: 참여 확정자에게 '모임이 취소되었습니다' 알림 발송
        // TODO: 대기자에게도 '모임이 취소되었습니다' 알림 발송
        // TODO: 미확정 지원자에게 '모임이 취소되어 지원이 취소되었습니다' 알림 발송
        log.info("모임 수동 취소: activityId={}, by userId={}", activityId, userId);
    }

    /**
     * 모임 시작 시점이 지났는데 일반 참여자(PARTICIPANT)가 한 명도 CONFIRMED가 아닌 모임을 자동 취소.
     * app-worker 스케줄러에서 주기적으로 호출.
     */
    @Transactional
    public int cancelActivitiesAutomatically() {
        List<Activity> candidates = activityRepository.findByLifecycleStatusAndStartAtBefore(
                ActivityLifecycleStatus.RECRUITING, OffsetDateTime.now());

        int cancelledCount = 0;
        for (Activity activity : candidates) {
            // capacity 0명 모임은 자동 취소 대상 제외
            if (activity.getCapacity() != null && activity.getCapacity() == 0) {
                continue;
            }

            // PARTICIPANT role 중 CONFIRMED 상태인 사람이 있는지 확인 (LEADER/MANAGER 제외)
            long confirmedParticipants = activityParticipantRepository
                    .countByActivityIdAndStatusAndParticipantStatusAndRole(
                            activity.getId(), BaseStatus.ACTIVE,
                            ParticipantStatus.CONFIRMED, ParticipantRole.PARTICIPANT);

            if (confirmedParticipants == 0) {
                cancelActivity(activity);
                // TODO: 모임장/모임관리자에게 '참여 확정자가 없어 모임이 자동 취소되었습니다' 알림
                // TODO: 대기자/미확정 지원자에게 '모임이 취소되었습니다' 알림
                log.info("모임 자동 취소: activityId={}", activity.getId());
                cancelledCount++;
            }
        }
        return cancelledCount;
    }

    private void cancelActivity(Activity activity) {
        activity.cancel();

        // 해당 모임의 모든 활성 참여자를 CANCELLED 처리
        List<ActivityParticipant> participants = activityParticipantRepository
                .findByActivityId(activity.getId()).stream()
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .toList();

        for (ActivityParticipant p : participants) {
            if (p.getParticipantStatus() != ParticipantStatus.CANCELLED
                    && p.getParticipantStatus() != ParticipantStatus.REJECTED
                    && p.getParticipantStatus() != ParticipantStatus.DECLINED) {
                p.updateParticipantStatus(ParticipantStatus.CANCELLED);
            }
        }
    }

    // ========== 모임 종료 ==========

    /**
     * 종료 시점(endAt)이 지난 RECRUITING/IN_PROGRESS 모임을 ENDED로 전환.
     * app-worker 스케줄러에서 주기적으로 호출.
     */
    @Transactional
    public int endActivitiesAutomatically() {
        List<Activity> candidates = activityRepository.findByLifecycleStatusInAndEndAtBefore(
                List.of(ActivityLifecycleStatus.RECRUITING, ActivityLifecycleStatus.IN_PROGRESS),
                OffsetDateTime.now());

        for (Activity activity : candidates) {
            activity.markEnded();

            // 대기열 소멸
            List<ActivityParticipant> waitlist = activityParticipantRepository
                    .findByActivityIdAndStatusAndParticipantStatusAndRoleOrderByWaitlistOrderAsc(
                            activity.getId(), BaseStatus.ACTIVE,
                            ParticipantStatus.PENDING, ParticipantRole.PARTICIPANT);
            for (ActivityParticipant waiter : waitlist) {
                waiter.updateParticipantStatus(ParticipantStatus.CANCELLED);
                // TODO: 대기자에게 '대기가 종료되었습니다' 알림
            }

            // TODO: 별점 평가 요청 알림 발송 (참여 확정자들에게)
            // TODO: 지원서 보관 결정 안내 알림 발송
            // TODO: 출석기록 이관 (동아리/연합회 → 출석부, 개인 → 삭제)
            // TODO: 참여자의 모임 이력에 자동 기록
            log.info("모임 종료 처리: activityId={}, 대기열 소멸 {}명", activity.getId(), waitlist.size());
        }

        return candidates.size();
    }

    /**
     * ENDED 상태에서 7일 경과한 모임을 DELETED로 전환.
     * app-worker 스케줄러에서 주기적으로 호출.
     */
    @Transactional
    public int deleteEndedActivitiesAutomatically() {
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
        List<Activity> candidates = activityRepository.findByLifecycleStatusAndEndAtBefore(
                ActivityLifecycleStatus.ENDED, sevenDaysAgo);

        for (Activity activity : candidates) {
            activity.markDeleted();
            // TODO: 보관 미결정 지원서 응답 삭제
            // TODO: 문의/공지 데이터 삭제
            log.info("종료 모임 삭제: activityId={}", activity.getId());
        }

        return candidates.size();
    }

    @Transactional
    public void addActivityInterest(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        long currentCount = activityInterestRepository.countByUserIdAndStatus(userId, BaseStatus.ACTIVE);
        if (currentCount >= 20) {
            throw new BusinessException(ActivityErrorCode.INTEREST_LIMIT_EXCEEDED);
        }

        activityInterestRepository.findByActivityIdAndUserId(activityId, userId)
                .ifPresentOrElse(
                        interest -> {
                            if (interest.getStatus() == BaseStatus.ACTIVE) {
                                throw new BusinessException(ActivityErrorCode.ACTIVITY_INTEREST_ALREADY_EXISTS);
                            }
                            interest.activate();
                        },
                        () -> {
                            ActivityInterest interest = ActivityInterest.builder()
                                    .activity(activity)
                                    .user(user)
                                    .build();
                            activityInterestRepository.save(interest);
                        }
                );

        activity.increaseLikeCount();
        log.info("관심 모임 추가: userId={}, activityId={}", userId, activityId);
    }

    @Transactional
    public void removeActivityInterest(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);

        ActivityInterest interest = activityInterestRepository.findByActivityIdAndUserId(activityId, userId)
                .filter(i -> i.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_INTEREST_NOT_FOUND));

        interest.inactivate();
        activity.decreaseLikeCount();
        log.info("관심 모임 제거: userId={}, activityId={}", userId, activityId);
    }

    @Transactional(readOnly = true)
    public Slice<ActivitySummaryResult> getInterestedActivities(Long userId, Pageable pageable) {
        return activityRepository.findInterestedActivities(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Slice<ActivityHistoryResult> getActivityHistory(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return activityParticipantRepository.findActivityHistoryByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public String getInviteCode(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);

        if (activity.getOpenType() != OpenType.PRIVATE) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_NOT_PRIVATE);
        }

        if (activity.getClub() != null) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_NOT_USER_CREATED);
        }

        if (!activity.getCreator().getId().equals(userId)) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_INVITE_PERMISSION_DENIED);
        }

        return activity.getInviteCode();
    }

    @Transactional
    public void joinByInviteCode(Long userId, String inviteCode) {
        Activity activity = activityRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.INVITE_CODE_NOT_FOUND));

        validateActivityNotCancelledOrDeleted(activity);

        if (activity.getCreator().getId().equals(userId)) {
            throw new BusinessException(ActivityErrorCode.CANNOT_JOIN_OWN_ACTIVITY);
        }

        activityParticipantRepository.findByActivityIdAndParticipantId(activity.getId(), userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .ifPresent(p -> {
                    throw new BusinessException(ActivityErrorCode.ALREADY_PARTICIPANT);
                });

        if (activity.getCapacity() != null) {
            long currentCount = activityParticipantRepository.countByActivityIdAndStatusAndParticipantStatusIn(
                    activity.getId(),
                    BaseStatus.ACTIVE,
                    List.of(ParticipantStatus.PENDING, ParticipantStatus.APPROVED, ParticipantStatus.CONFIRMED)
            );
            if (currentCount >= activity.getCapacity()) {
                throw new BusinessException(ActivityErrorCode.ACTIVITY_FULL);
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ActivityParticipant participant = ActivityParticipant.builder()
                .activity(activity)
                .participant(user)
                .build();

        activityParticipantRepository.save(participant);
        log.info("초대 코드로 모임 참여: userId={}, activityId={}", userId, activity.getId());
    }

    // ========== 모임관리자 ==========

    private static final int MAX_MANAGERS = 50;

    @Transactional(readOnly = true)
    public String getManagerInviteCode(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);
        validateLeader(activity, userId);
        return activity.getManagerInviteCode();
    }

    @Transactional
    public void joinAsManager(Long userId, String managerInviteCode) {
        Activity activity = activityRepository.findByManagerInviteCode(managerInviteCode)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.INVITE_CODE_NOT_FOUND));

        validateActivityNotCancelledOrDeleted(activity);

        if (activity.getCreator().getId().equals(userId)) {
            throw new BusinessException(ActivityErrorCode.CANNOT_JOIN_OWN_ACTIVITY);
        }

        // 이미 참여자인지 확인
        activityParticipantRepository.findByActivityIdAndParticipantId(activity.getId(), userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .ifPresent(p -> {
                    if (p.getRole() == ParticipantRole.MANAGER) {
                        throw new BusinessException(ActivityErrorCode.ALREADY_MANAGER);
                    }
                    throw new BusinessException(ActivityErrorCode.ALREADY_PARTICIPANT);
                });

        // 매니저 50명 제한
        long managerCount = activityParticipantRepository.countByActivityIdAndStatusAndRole(
                activity.getId(), BaseStatus.ACTIVE, ParticipantRole.MANAGER);
        if (managerCount >= MAX_MANAGERS) {
            throw new BusinessException(ActivityErrorCode.MANAGER_LIMIT_EXCEEDED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 모임관리자는 자동 참여 확정 + MANAGER role (capacity에 미포함)
        ActivityParticipant manager = ActivityParticipant.builder()
                .activity(activity)
                .participant(user)
                .participantStatus(ParticipantStatus.CONFIRMED)
                .role(ParticipantRole.MANAGER)
                .build();
        activityParticipantRepository.save(manager);

        log.info("모임관리자 등록: userId={}, activityId={}", userId, activity.getId());
    }

    @Transactional
    public void removeManager(Long userId, Long activityId, Long managerId) {
        Activity activity = findActiveActivity(activityId);
        validateLeader(activity, userId);

        ActivityParticipant manager = activityParticipantRepository
                .findByActivityIdAndParticipantIdAndStatusAndRole(
                        activityId, managerId, BaseStatus.ACTIVE, ParticipantRole.MANAGER)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.MANAGER_NOT_FOUND));

        // 자격 해제 시 참여 확정도 함께 취소
        manager.inactivate();
        log.info("모임관리자 제거: managerId={}, activityId={}", managerId, activityId);
    }

    @Transactional
    public void leaveAsManager(Long userId, Long activityId) {
        Activity activity = findActiveActivity(activityId);

        ActivityParticipant manager = activityParticipantRepository
                .findByActivityIdAndParticipantIdAndStatusAndRole(
                        activityId, userId, BaseStatus.ACTIVE, ParticipantRole.MANAGER)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.MANAGER_NOT_FOUND));

        // 자격 해제 시 참여 확정도 함께 취소
        manager.inactivate();
        log.info("모임관리자 자발적 탈퇴: userId={}, activityId={}", userId, activityId);
    }

    // ========== 헬퍼 ==========

    private Activity findActiveActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));
        if (activity.getStatus() == BaseStatus.INACTIVE) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }
        if (activity.getLifecycleStatus() == ActivityLifecycleStatus.CANCELLED
                || activity.getLifecycleStatus() == ActivityLifecycleStatus.DELETED) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_ALREADY_CANCELLED);
        }
        return activity;
    }

    private void validateLeader(Activity activity, Long userId) {
        if (!activity.getCreator().getId().equals(userId)) {
            throw new BusinessException(ActivityErrorCode.NOT_ACTIVITY_LEADER);
        }
    }

    private void validateActivityNotCancelledOrDeleted(Activity activity) {
        if (activity.getStatus() == BaseStatus.INACTIVE
                || activity.getLifecycleStatus() == ActivityLifecycleStatus.CANCELLED
                || activity.getLifecycleStatus() == ActivityLifecycleStatus.DELETED) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }
    }

    /**
     * 모임장 또는 모임관리자인지 확인
     */
    private boolean isLeaderOrManager(Long activityId, Long userId) {
        return activityParticipantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .map(p -> p.getRole() == ParticipantRole.LEADER || p.getRole() == ParticipantRole.MANAGER)
                .orElse(false);
    }
}
