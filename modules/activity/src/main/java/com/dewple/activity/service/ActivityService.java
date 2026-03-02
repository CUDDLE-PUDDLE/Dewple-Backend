package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.club.entity.ClubMember;
import com.dewple.club.exception.ClubErrorCode;
import com.dewple.club.repository.ClubMemberRepository;
import com.dewple.club.repository.ClubRepository;
import com.dewple.common.entity.Club;
import com.dewple.common.entity.User;
import com.dewple.common.enums.Permission;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewple.common.enums.BaseStatus;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

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
                activity.getCreatedAt()
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
}
