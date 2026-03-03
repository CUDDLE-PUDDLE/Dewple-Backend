package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.exception.ActivityErrorCode;
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
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewple.common.enums.BaseStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityParticipantRepository activityParticipantRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public CreateActivityResult createActivity(Long userId, CreateActivityParam param) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (param.endAt().isBefore(param.startAt()) || param.endAt().isEqual(param.startAt())) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_END_BEFORE_START);
        }

        if (param.startAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_START_IN_PAST);
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

        Activity activity = Activity.builder()
                .club(club)
                .creator(creator)
                .openType(param.openType())
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
                .build();

        activityRepository.save(activity);
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
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (activity.getStatus() == BaseStatus.INACTIVE) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_ALREADY_INACTIVE);
        }

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

        activity.inactivate();
        log.info("모임 삭제 완료: activityId={}, userId={}", activityId, userId);
    }

    @Transactional(readOnly = true)
    public GetActivityDetailResult getActivityDetail(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        if (activity.getStatus() == BaseStatus.INACTIVE) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }

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
}
