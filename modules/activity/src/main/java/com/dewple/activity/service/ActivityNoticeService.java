package com.dewple.activity.service;

import com.dewple.activity.entity.Activity;
import com.dewple.activity.entity.ActivityNotice;
import com.dewple.activity.entity.ActivityNoticeComment;
import com.dewple.activity.entity.ActivityParticipant;
import com.dewple.activity.exception.ActivityErrorCode;
import com.dewple.activity.repository.ActivityNoticeCommentRepository;
import com.dewple.activity.repository.ActivityNoticeRepository;
import com.dewple.activity.repository.ActivityParticipantRepository;
import com.dewple.activity.repository.ActivityRepository;
import com.dewple.common.entity.User;
import com.dewple.common.enums.BaseStatus;
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
public class ActivityNoticeService {

    private final ActivityRepository activityRepository;
    private final ActivityNoticeRepository noticeRepository;
    private final ActivityNoticeCommentRepository commentRepository;
    private final ActivityPermissionValidator permissionValidator;
    private final UserRepository userRepository;

    @Transactional
    public ActivityNotice createNotice(Long userId, Long activityId, String title, String content, String imageUrls) {
        Activity activity = findActiveActivity(activityId);
        permissionValidator.validateLeaderOrManager(activityId, userId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ActivityNotice notice = ActivityNotice.builder()
                .activity(activity)
                .author(author)
                .title(title)
                .content(content)
                .imageUrls(imageUrls)
                .build();

        noticeRepository.save(notice);
        // TODO: 참여 확정자에게 공지 알림 발송
        log.info("모임 공지 생성: noticeId={}, activityId={}", notice.getId(), activityId);
        return notice;
    }

    @Transactional
    public void updateNotice(Long userId, Long activityId, Long noticeId,
                             String title, String content, String imageUrls) {
        findActiveActivity(activityId);
        permissionValidator.validateLeaderOrManager(activityId, userId);

        ActivityNotice notice = findActiveNotice(noticeId, activityId);
        notice.update(title, content, imageUrls);
        log.info("모임 공지 수정: noticeId={}", noticeId);
    }

    @Transactional
    public void deleteNotice(Long userId, Long activityId, Long noticeId) {
        findActiveActivity(activityId);
        permissionValidator.validateLeaderOrManager(activityId, userId);

        ActivityNotice notice = findActiveNotice(noticeId, activityId);
        notice.inactivate();
        log.info("모임 공지 삭제: noticeId={}", noticeId);
    }

    @Transactional(readOnly = true)
    public Slice<ActivityNotice> getNoticeList(Long userId, Long activityId, Pageable pageable) {
        findActiveActivity(activityId);
        permissionValidator.validateConfirmedParticipant(activityId, userId);

        return noticeRepository.findByActivityIdAndStatusOrderByCreatedAtDesc(
                activityId, BaseStatus.ACTIVE, pageable);
    }

    @Transactional
    public ActivityNoticeComment createComment(Long userId, Long activityId, Long noticeId, String content) {
        findActiveActivity(activityId);
        permissionValidator.validateConfirmedParticipant(activityId, userId);

        ActivityNotice notice = findActiveNotice(noticeId, activityId);
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ActivityNoticeComment comment = ActivityNoticeComment.builder()
                .notice(notice)
                .author(author)
                .content(content)
                .build();

        commentRepository.save(comment);
        notice.increaseCommentCount();
        // TODO: 댓글 알림 (모임장/관리자에게, 누적 1/5/10/20/40... 도달 시)
        log.info("모임 공지 댓글 생성: commentId={}, noticeId={}", comment.getId(), noticeId);
        return comment;
    }

    @Transactional
    public void deleteComment(Long userId, Long activityId, Long noticeId, Long commentId) {
        findActiveActivity(activityId);

        ActivityNoticeComment comment = commentRepository.findById(commentId)
                .filter(c -> c.getNotice().getId().equals(noticeId))
                .filter(c -> c.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        boolean isLeaderOrManager = permissionValidator.isLeaderOrManager(activityId, userId);

        if (!isAuthor && !isLeaderOrManager) {
            throw new BusinessException(ActivityErrorCode.ACTIVITY_DELETE_PERMISSION_DENIED);
        }

        comment.inactivate();
        findActiveNotice(noticeId, activityId).decreaseCommentCount();
        log.info("모임 공지 댓글 삭제: commentId={}", commentId);
    }

    @Transactional(readOnly = true)
    public Slice<ActivityNoticeComment> getCommentList(Long userId, Long activityId,
                                                       Long noticeId, Pageable pageable) {
        findActiveActivity(activityId);
        permissionValidator.validateConfirmedParticipant(activityId, userId);

        return commentRepository.findByNoticeIdAndStatusOrderByCreatedAtAsc(
                noticeId, BaseStatus.ACTIVE, pageable);
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

    private ActivityNotice findActiveNotice(Long noticeId, Long activityId) {
        return noticeRepository.findById(noticeId)
                .filter(n -> n.getActivity().getId().equals(activityId))
                .filter(n -> n.getStatus() == BaseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ActivityErrorCode.ACTIVITY_NOT_FOUND));
    }

}
