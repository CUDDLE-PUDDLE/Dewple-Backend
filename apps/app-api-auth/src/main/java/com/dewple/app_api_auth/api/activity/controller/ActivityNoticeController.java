package com.dewple.app_api_auth.api.activity.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.activity.entity.ActivityNotice;
import com.dewple.activity.entity.ActivityNoticeComment;
import com.dewple.activity.service.ActivityNoticeService;
import com.dewple.app_api_auth.api.activity.dto.CreateActivityNoticeRequest;
import com.dewple.app_api_auth.api.activity.dto.CreateCommentRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.response.SliceResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Activity - 공지", description = "모임 참여자 공지 API")
@RestController
@RequestMapping("/activities/{activityId}/notices")
@RequiredArgsConstructor
public class ActivityNoticeController {

    private final ActivityNoticeService noticeService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "공지 목록 조회", description = "참여 확정자만 조회 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping
    public ApiResponse<SliceResponse<ActivityNoticeResponse>> getNoticeList(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Slice<ActivityNotice> notices = noticeService.getNoticeList(userId, activityId, pageable);
        Slice<ActivityNoticeResponse> response = notices.map(ActivityNoticeResponse::from);
        return ApiResponse.ok(SliceResponse.from(response));
    }

    @Operation(summary = "공지 생성", description = "모임장 또는 모임관리자만 생성 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ActivityNoticeResponse> createNotice(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody CreateActivityNoticeRequest request
    ) {
        String imageUrlsJson = toJson(request.imageUrls());
        ActivityNotice notice = noticeService.createNotice(
                userId, activityId, request.title(), request.content(), imageUrlsJson);
        return ApiResponse.ok(ActivityNoticeResponse.from(notice));
    }

    @Operation(summary = "공지 수정", description = "모임장 또는 모임관리자만 수정 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{noticeId}")
    public ApiResponse<Void> updateNotice(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long noticeId,
            @Valid @RequestBody CreateActivityNoticeRequest request
    ) {
        String imageUrlsJson = toJson(request.imageUrls());
        noticeService.updateNotice(userId, activityId, noticeId,
                request.title(), request.content(), imageUrlsJson);
        return ApiResponse.ok();
    }

    @Operation(summary = "공지 삭제", description = "모임장 또는 모임관리자만 삭제 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(userId, activityId, noticeId);
        return ApiResponse.ok();
    }

    // ========== 댓글 ==========

    @Operation(summary = "댓글 목록 조회", description = "참여 확정자만 조회 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/{noticeId}/comments")
    public ApiResponse<SliceResponse<ActivityNoticeCommentResponse>> getCommentList(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long noticeId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Slice<ActivityNoticeComment> comments = noticeService.getCommentList(
                userId, activityId, noticeId, pageable);
        Slice<ActivityNoticeCommentResponse> response = comments.map(ActivityNoticeCommentResponse::from);
        return ApiResponse.ok(SliceResponse.from(response));
    }

    @Operation(summary = "댓글 작성", description = "참여 확정자만 작성 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{noticeId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createComment(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long noticeId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        noticeService.createComment(userId, activityId, noticeId, request.content());
        return ApiResponse.ok();
    }

    @Operation(summary = "댓글 삭제", description = "작성자 본인 또는 모임장/모임관리자만 삭제 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{noticeId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long noticeId,
            @PathVariable Long commentId
    ) {
        noticeService.deleteComment(userId, activityId, noticeId, commentId);
        return ApiResponse.ok();
    }

    // ========== 내부 DTO ==========

    public record ActivityNoticeResponse(
            Long id, String title, String content, String imageUrls,
            Long authorId, String authorName, int commentCount
    ) {
        public static ActivityNoticeResponse from(ActivityNotice notice) {
            return new ActivityNoticeResponse(
                    notice.getId(), notice.getTitle(), notice.getContent(),
                    notice.getImageUrls(), notice.getAuthor().getId(),
                    notice.getAuthor().getName(), notice.getCommentCount());
        }
    }

    public record ActivityNoticeCommentResponse(
            Long id, String content, Long authorId, String authorName
    ) {
        public static ActivityNoticeCommentResponse from(ActivityNoticeComment comment) {
            return new ActivityNoticeCommentResponse(
                    comment.getId(), comment.getContent(),
                    comment.getAuthor().getId(), comment.getAuthor().getName());
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
