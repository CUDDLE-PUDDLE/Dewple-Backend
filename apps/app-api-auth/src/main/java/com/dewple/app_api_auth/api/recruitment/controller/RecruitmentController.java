package com.dewple.app_api_auth.api.recruitment.controller;

import com.dewple.app_api_auth.api.recruitment.dto.CreateRecruitmentRequest;
import com.dewple.app_api_auth.api.recruitment.dto.RecruitmentPostingDetailResponse;
import com.dewple.app_api_auth.api.recruitment.dto.RecruitmentPostingListResponse;
import com.dewple.app_api_auth.api.recruitment.dto.RecruitmentPostingResponse;
import com.dewple.app_api_auth.api.recruitment.dto.UpdateRecruitmentRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.recruitment.entity.RecruitmentPosting;
import com.dewple.recruitment.service.PublicPostingDetailResult;
import com.dewple.recruitment.service.PublicPostingListResult;
import com.dewple.recruitment.service.RecruitmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "Recruitment - 공고", description = "공고 API")
@RestController
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "모집 공고 생성", description = "모집 공고를 생성하고 즉시 발행합니다.")
    @PostMapping("/clubs/{clubId}/recruitment-posts")
    public ResponseEntity<ApiResponse<RecruitmentPostingResponse>> createRecruitment(
            @PathVariable Long clubId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRecruitmentRequest request) throws JsonProcessingException {

        Long creatorId = Long.parseLong(jwt.getSubject());

        RecruitmentPosting posting = recruitmentService.createRecruitment(clubId, creatorId, request.toCommand(objectMapper));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(posting)));
    }

    @Operation(summary = "모집 공고 임시 저장", description = "모집 공고를 초안 상태로 임시 저장합니다. postingId가 없으면 새 초안을 생성하고, 있으면 기존 초안을 업데이트합니다.")
    @PutMapping("/clubs/{clubId}/recruitment-posts")
    public ResponseEntity<ApiResponse<RecruitmentPostingResponse>> temporaryStorageRecruitment(
            @PathVariable Long clubId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long postingId,
            @Valid @RequestBody CreateRecruitmentRequest request) throws JsonProcessingException {

        Long creatorId = Long.parseLong(jwt.getSubject());

        RecruitmentPosting posting = recruitmentService.temporaryStorageRecruitment(clubId, creatorId, postingId, request.toCommand(objectMapper));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(posting)));
    }

    @Operation(summary = "모집 공고 발행", description = "임시 저장된 초안 공고를 발행합니다.")
    @PostMapping("/clubs/{clubId}/recruitment-posts/{postingId}/publish")
    public ResponseEntity<ApiResponse<RecruitmentPostingResponse>> publishRecruitment(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt) {

        Long creatorId = Long.parseLong(jwt.getSubject());

        RecruitmentPosting posting = recruitmentService.publishRecruitment(clubId, creatorId, postingId);

        return ResponseEntity.ok(ApiResponse.ok(toResponse(posting)));
    }

    @Operation(summary = "모집 공고 수정", description = "발행된 모집 공고의 제목, 본문, 지원서 양식을 수정합니다.\n사용자가 작성자랑 달라도, 공고 권한이 있으면 수정 가능합니다.\n컴포넌트의 삭제는 불가합니다.")
    @PatchMapping("/clubs/{clubId}/recruitment-posts/{postingId}")
    public ResponseEntity<ApiResponse<RecruitmentPostingResponse>> updateRecruitment(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateRecruitmentRequest request) throws JsonProcessingException {

        Long creatorId = Long.parseLong(jwt.getSubject());

        RecruitmentPosting posting = recruitmentService.updateRecruitment(clubId, creatorId, postingId, request.toCommand(objectMapper));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(posting)));
    }

    @Operation(summary = "이전 공고 지원 양식 불러오기", description = "해당 동아리의 가장 최근 공고(DRAFT 제외)에서 최신 버전의 지원 양식을 반환합니다.")
    @GetMapping("/clubs/{clubId}/recruitment-posts/previous-form")
    public ResponseEntity<ApiResponse<String>> getPreviousApplicationForm(
            @PathVariable Long clubId,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        String applicationForm = recruitmentService.getPreviousApplicationForm(clubId, userId);

        return ResponseEntity.ok(ApiResponse.ok(applicationForm));
    }

    @Operation(summary = "모집 마감일 변경", description = "모집 공고의 마감일을 변경합니다. 최대 2회까지 변경 가능합니다.")
    @PatchMapping("/clubs/{clubId}/recruitment-posts/{postingId}/deadline")
    public ResponseEntity<ApiResponse<RecruitmentPostingResponse>> changeDeadline(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ChangeDeadlineRequest request) {

        Long userId = Long.parseLong(jwt.getSubject());

        RecruitmentPosting posting = recruitmentService.changeDeadline(clubId, userId, postingId, request.endAt());

        return ResponseEntity.ok(ApiResponse.ok(toResponse(posting)));
    }

    public record ChangeDeadlineRequest(
            @NotNull(message = "변경할 마감일은 필수입니다.")
            OffsetDateTime endAt
    ) {}

    @Operation(summary = "모집 공고 조기 마감", description = "마감 기한 전에 모집 공고를 조기 마감합니다. 합격예비자가 있으면 추가합격 기간(1~14일)을 함께 설정해야 합니다.")
    @PostMapping("/clubs/{clubId}/recruitment-posts/{postingId}/close")
    public ResponseEntity<ApiResponse<RecruitmentPostingResponse>> closeRecruitment(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) CloseRecruitmentRequest request) {

        Long userId = Long.parseLong(jwt.getSubject());
        Integer additionalDays = request != null ? request.additionalAcceptanceDays() : null;

        RecruitmentPosting posting = recruitmentService.closeRecruitment(clubId, userId, postingId, additionalDays);

        return ResponseEntity.ok(ApiResponse.ok(toResponse(posting)));
    }

    public record CloseRecruitmentRequest(
            Integer additionalAcceptanceDays
    ) {}

    @Operation(summary = "모집 공고 조회수 증가", description = "모집 공고 조회수를 1 증가시킵니다. 클라이언트가 5초 이상 페이지에 머문 뒤 호출합니다. 인증 불필요.")
    @PostMapping("/recruitment-posts/{postingId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(
            @PathVariable Long postingId) {

        recruitmentService.incrementViewCount(postingId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "모집 공고 목록 조회", description = "동아리의 모집 공고 목록을 조회합니다. status=OPEN(모집 중) 또는 status=CLOSED(마감)로 필터링합니다. 인증 불필요.")
    @GetMapping("/clubs/{clubId}/recruitment-posts")
    public ResponseEntity<ApiResponse<List<RecruitmentPostingListResponse>>> getPublicPostingList(
            @PathVariable Long clubId,
            @RequestParam String status) {

        List<PublicPostingListResult> results = recruitmentService.getPublicPostingList(clubId, status);

        List<RecruitmentPostingListResponse> response = results.stream()
                .map(this::toListResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "모집 공고 상세 조회", description = "동아리의 특정 모집 공고를 상세 조회합니다. DRAFT 상태는 조회 불가. 인증 불필요.")
    @GetMapping("/clubs/{clubId}/recruitment-posts/{postingId}")
    public ResponseEntity<ApiResponse<RecruitmentPostingDetailResponse>> getPublicPostingDetail(
            @PathVariable Long clubId,
            @PathVariable Long postingId) {

        PublicPostingDetailResult result = recruitmentService.getPublicPostingDetail(clubId, postingId);

        return ResponseEntity.ok(ApiResponse.ok(toDetailResponse(result)));
    }

    @Operation(summary = "모집 공고 삭제", description = "모집 공고를 삭제합니다. 사용자가 작성자랑 달라도, 공고 권한이 있으면 삭제 가능합니다.")
    @DeleteMapping("/clubs/{clubId}/recruitment-posts/{postingId}")
    public ResponseEntity<ApiResponse<RecruitmentPostingResponse>> deleteRecruitment(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt) {

        Long creatorId = Long.parseLong(jwt.getSubject());

        RecruitmentPosting posting = recruitmentService.deleteRecruitment(clubId, creatorId, postingId);

        return ResponseEntity.ok(ApiResponse.ok(toResponse(posting)));
    }

    private RecruitmentPostingResponse toResponse(RecruitmentPosting posting) {
        return new RecruitmentPostingResponse(posting.getId(), posting.getRecentRecruitmentVersion());
    }

    private RecruitmentPostingListResponse toListResponse(PublicPostingListResult result) {
        return new RecruitmentPostingListResponse(
                result.postingId(),
                result.generationNo(),
                result.title(),
                result.endAt(),
                result.departments(),
                result.viewCount(),
                result.recruitmentStatus()
        );
    }

    private RecruitmentPostingDetailResponse toDetailResponse(PublicPostingDetailResult result) {
        List<RecruitmentPostingDetailResponse.DepartmentInfo> departments = result.departments().stream()
                .map(d -> new RecruitmentPostingDetailResponse.DepartmentInfo(d.name(), d.count()))
                .toList();

        List<RecruitmentPostingDetailResponse.ProcessInfo> processes = result.processes().stream()
                .map(p -> new RecruitmentPostingDetailResponse.ProcessInfo(
                        p.processOrder(), p.name(), p.processType(), p.startAt(), p.endAt()))
                .toList();

        return new RecruitmentPostingDetailResponse(
                result.postingId(),
                result.clubId(),
                result.clubName(),
                result.generationNo(),
                result.title(),
                result.content(),
                result.themeColor(),
                result.capacity(),
                result.startAt(),
                result.endAt(),
                result.resultDate(),
                result.endOfGenerationDate(),
                result.hasSecondInterview(),
                result.emergencyContact(),
                result.firstAnnouncementDate(),
                departments,
                processes,
                result.applicationForm(),
                result.viewCount(),
                result.recruitmentStatus()
        );
    }
}
