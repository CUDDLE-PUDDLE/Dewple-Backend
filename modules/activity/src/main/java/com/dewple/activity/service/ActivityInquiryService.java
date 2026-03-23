package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityInquiry;
import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityInquiryRepository;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
import com.dewple.common.enums.ParticipantRole;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityInquiryService {

    private final ActivityRepository activityRepository;
    private final ActivityInquiryRepository inquiryRepository;
    private final ActivityParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional
    public ActivityInquiry createInquiry(Long userId, Long activityId, String content, Boolean isAnonymous) {
        Activity activity = findActiveActivity(activityId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ActivityInquiry inquiry = ActivityInquiry.builder()
                .activity(activity)
                .author(author)
                .content(content)
                .isAnonymous(isAnonymous)
                .build();

        inquiryRepository.save(inquiry);
        // TODO: 모임장/모임관리자에게 문의 알림 (누적 1/5/10/20/40/80... 도달 시)
        log.info("모임 문의 작성: inquiryId={}, activityId={}", inquiry.getId(), activityId);
        return inquiry;
    }

    @Transactional
    public void updateInquiry(Long userId, Long activityId, Long inquiryId, String content) {
        findActiveActivity(activityId);

        ActivityInquiry inquiry = findActiveInquiry(inquiryId, activityId);
        validateInquiryAuthor(inquiry, userId);

        if (inquiry.hasAnswer()) {
            throw new BusinessException(ActivityErrorCode.INQUIRY_ALREADY_ANSWERED);
        }

        inquiry.updateContent(content);
        log.info("모임 문의 수정: inquiryId={}", inquiryId);
    }

    @Transactional
    public void deleteInquiryByAuthor(Long userId, Long activityId, Long inquiryId) {
        findActiveActivity(activityId);

        ActivityInquiry inquiry = findActiveInquiry(inquiryId, activityId);
        validateInquiryAuthor(inquiry, userId);

        if (inquiry.hasAnswer()) {
            throw new BusinessException(ActivityErrorCode.INQUIRY_ALREADY_ANSWERED);
        }

        inquiry.inactivate();
        log.info("모임 문의 삭제 (작성자): inquiryId={}", inquiryId);
    }

    @Transactional
    public void deleteInquiryByManager(Long userId, Long activityId, Long inquiryId) {
        findActiveActivity(activityId);
        validateLeaderOrManager(activityId, userId);

        ActivityInquiry inquiry = findActiveInquiry(inquiryId, activityId);
        inquiry.inactivate();
        log.info("모임 문의 삭제 (운영진): inquiryId={}, by userId={}", inquiryId, userId);
    }

    @Transactional
    public void answerInquiry(Long userId, Long activityId, Long inquiryId, String answer) {
        findActiveActivity(activityId);
        validateLeaderOrManager(activityId, userId);

        ActivityInquiry inquiry = findActiveInquiry(inquiryId, activityId);

        User answerer = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        inquiry.setAnswer(answer, answerer);
        log.info("모임 문의 답변: inquiryId={}, by userId={}", inquiryId, userId);
    }

    @Transactional
    public void updateAnswer(Long userId, Long activityId, Long inquiryId, String answer) {
        findActiveActivity(activityId);
        validateLeaderOrManager(activityId, userId);

        ActivityInquiry inquiry = findActiveInquiry(inquiryId, activityId);
        if (!inquiry.hasAnswer()) {
            throw new BusinessException(ActivityErrorCode.INQUIRY_NOT_ANSWERED);
        }

        inquiry.updateAnswer(answer);
        log.info("모임 문의 답변 수정: inquiryId={}", inquiryId);
    }

    @Transactional(readOnly = true)
    public Slice<ActivityInquiry> getInquiryList(Long activityId, Pageable pageable) {
        findActiveActivity(activityId);
        return inquiryRepository.findByActivityIdAndStatusOrderByCreatedAtDesc(
                activityId, BaseStatus.ACTIVE, pageable);
    }

    // ========== 헬퍼 ==========

    private Activity findActiveActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));
        if (activity.getStatus() == BaseStatus.INACTIVE) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    private ActivityInquiry findActiveInquiry(Long inquiryId, Long activityId) {
        return inquiryRepository.findById(inquiryId)
                .filter(i -> i.getActivity().getId().equals(activityId))
                .filter(i -> i.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.INQUIRY_NOT_FOUND));
    }

    private void validateInquiryAuthor(ActivityInquiry inquiry, Long userId) {
        if (!inquiry.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED);
        }
    }

    private void validateLeaderOrManager(Long activityId, Long userId) {
        boolean isLeaderOrManager = participantRepository.findByActivityIdAndParticipantId(activityId, userId)
                .filter(p -> p.getStatus() == BaseStatus.ACTIVE)
                .map(p -> p.getRole() == ParticipantRole.LEADER || p.getRole() == ParticipantRole.MANAGER)
                .orElse(false);

        if (!isLeaderOrManager) {
            throw new BusinessException(ActivityErrorCode.NOT_ACTIVITY_LEADER);
        }
    }
}
