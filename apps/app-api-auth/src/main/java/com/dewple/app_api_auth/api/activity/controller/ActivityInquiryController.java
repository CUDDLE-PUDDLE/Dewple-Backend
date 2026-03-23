package com.dewple.app_api_auth.api.activity.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.activity.entity.ActivityInquiry;
import com.dewple.activity.service.ActivityInquiryService;
import com.dewple.app_api_auth.api.activity.dto.AnswerInquiryRequest;
import com.dewple.app_api_auth.api.activity.dto.CreateInquiryRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.response.SliceResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
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

@Tag(name = "Activity Inquiry", description = "모임 문의 API")
@RestController
@RequestMapping("/activities/{activityId}/inquiries")
@RequiredArgsConstructor
public class ActivityInquiryController {

    private final ActivityInquiryService inquiryService;

    @Operation(summary = "문의 목록 조회", description = "모임의 문의 목록을 조회합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping
    public ApiResponse<SliceResponse<InquiryResponse>> getInquiryList(
            @PathVariable Long activityId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Slice<ActivityInquiry> inquiries = inquiryService.getInquiryList(activityId, pageable);
        Slice<InquiryResponse> response = inquiries.map(InquiryResponse::from);
        return ApiResponse.ok(SliceResponse.from(response));
    }

    @Operation(summary = "문의 작성", description = "회원이 모임에 문의를 남깁니다. 익명/실명 선택 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createInquiry(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody CreateInquiryRequest request
    ) {
        inquiryService.createInquiry(userId, activityId, request.content(), request.isAnonymous());
        return ApiResponse.ok();
    }

    @Operation(summary = "문의 수정 (작성자)", description = "답변이 달리기 전에만 수정 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{inquiryId}")
    public ApiResponse<Void> updateInquiry(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody CreateInquiryRequest request
    ) {
        inquiryService.updateInquiry(userId, activityId, inquiryId, request.content());
        return ApiResponse.ok();
    }

    @Operation(summary = "문의 삭제 (작성자)", description = "답변이 달리기 전에만 삭제 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{inquiryId}")
    public ApiResponse<Void> deleteInquiryByAuthor(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long inquiryId
    ) {
        inquiryService.deleteInquiryByAuthor(userId, activityId, inquiryId);
        return ApiResponse.ok();
    }

    @Operation(summary = "문의 삭제 (운영진)", description = "모임장/모임관리자가 문의를 삭제합니다. 답변 유무와 무관하게 삭제 가능합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{inquiryId}/admin")
    public ApiResponse<Void> deleteInquiryByManager(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long inquiryId
    ) {
        inquiryService.deleteInquiryByManager(userId, activityId, inquiryId);
        return ApiResponse.ok();
    }

    @Operation(summary = "문의 답변", description = "모임장/모임관리자가 문의에 답변합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping("/{inquiryId}/answer")
    public ApiResponse<Void> answerInquiry(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody AnswerInquiryRequest request
    ) {
        inquiryService.answerInquiry(userId, activityId, inquiryId, request.answer());
        return ApiResponse.ok();
    }

    @Operation(summary = "문의 답변 수정", description = "모임장/모임관리자가 답변을 수정합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/{inquiryId}/answer")
    public ApiResponse<Void> updateAnswer(
            @CurrentUserId Long userId,
            @PathVariable Long activityId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody AnswerInquiryRequest request
    ) {
        inquiryService.updateAnswer(userId, activityId, inquiryId, request.answer());
        return ApiResponse.ok();
    }

    // ========== 응답 DTO ==========

    public record InquiryResponse(
            Long id, String content, Boolean isAnonymous,
            Long authorId, String authorName,
            String answer, Long answeredById, String answeredByName
    ) {
        public static InquiryResponse from(ActivityInquiry inquiry) {
            String displayName = inquiry.getIsAnonymous() ? "익명" : inquiry.getAuthor().getName();
            Long displayAuthorId = inquiry.getIsAnonymous() ? null : inquiry.getAuthor().getId();

            return new InquiryResponse(
                    inquiry.getId(),
                    inquiry.getContent(),
                    inquiry.getIsAnonymous(),
                    displayAuthorId,
                    displayName,
                    inquiry.getAnswer(),
                    inquiry.getAnsweredBy() != null ? inquiry.getAnsweredBy().getId() : null,
                    inquiry.getAnsweredBy() != null ? inquiry.getAnsweredBy().getName() : null
            );
        }
    }
}
