package com.dewple.app_api_auth.api.recruitment.controller;

import com.dewple.app_api_auth.api.recruitment.dto.ApplicationResponse;
import com.dewple.app_api_auth.api.recruitment.dto.GuestApplicationRequest;
import com.dewple.app_api_auth.api.recruitment.dto.MyApplicationListResponse;
import com.dewple.app_api_auth.api.recruitment.dto.SubmitApplicationRequest;
import com.dewple.app_api_auth.api.recruitment.dto.UpdateGuestApplicationRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.service.ApplicationService;
import com.dewple.recruitment.service.MyApplicationListResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Application", description = "지원서 API")
@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "지원서 제출", description = "모집 공고에 지원서를 제출합니다. 임시저장된 지원서가 있으면 제출 상태로 전환합니다.")
    @PostMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitApplicationRequest request) throws JsonProcessingException {

        Long applicantId = Long.parseLong(jwt.getSubject());

        Application application = applicationService.submitApplication(
                clubId, postingId, applicantId,
                new ApplicationService.SubmitApplicationCommand(request.toAnswersJson(objectMapper)));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(application)));
    }

    @Operation(summary = "지원서 임시저장", description = "모집 공고에 지원서를 임시저장합니다. 기존 임시저장이 있으면 답변을 갱신합니다.")
    @PutMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications")
    public ResponseEntity<ApiResponse<ApplicationResponse>> temporarySaveApplication(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitApplicationRequest request) throws JsonProcessingException {

        Long applicantId = Long.parseLong(jwt.getSubject());

        Application application = applicationService.temporarySaveApplication(
                clubId, postingId, applicantId,
                new ApplicationService.SubmitApplicationCommand(request.toAnswersJson(objectMapper)));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(application)));
    }

    @Operation(summary = "지원서 수정", description = "제출 또는 임시저장된 지원서의 답변을 수정합니다. 수정 기간 내에만 가능합니다.")
    @PatchMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> editApplication(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitApplicationRequest request) throws JsonProcessingException {

        Long applicantId = Long.parseLong(jwt.getSubject());

        Application application = applicationService.editApplication(
                clubId, postingId, applicationId, applicantId,
                new ApplicationService.SubmitApplicationCommand(request.toAnswersJson(objectMapper)));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(application)));
    }

    @Operation(summary = "비회원 지원서 제출", description = "인증 없이 이름과 전화번호로 지원서를 제출합니다.")
    @PostMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications/guest")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitGuestApplication(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @Valid @RequestBody GuestApplicationRequest request) throws JsonProcessingException {

        Application application = applicationService.submitGuestApplication(
                clubId, postingId,
                new ApplicationService.GuestApplicationCommand(
                        request.guestPhone(), request.toAnswersJson(objectMapper)));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(application)));
    }

    @Operation(summary = "비회원 지원서 수정", description = "전화번호로 기존 지원서를 찾아 답변을 수정합니다.")
    @PatchMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications/guest")
    public ResponseEntity<ApiResponse<ApplicationResponse>> editGuestApplication(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @Valid @RequestBody UpdateGuestApplicationRequest request) throws JsonProcessingException {

        Application application = applicationService.editGuestApplication(
                clubId, postingId,
                new ApplicationService.GuestEditApplicationCommand(
                        request.guestPhone(), request.toAnswersJson(objectMapper)));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(application)));
    }

    @Operation(summary = "지원 철회", description = "제출 또는 임시저장된 지원서를 철회합니다. 합격/불합격 상태에서는 철회할 수 없습니다.")
    @DeleteMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}")
    public ResponseEntity<ApiResponse<Void>> withdrawApplication(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        Long applicantId = Long.parseLong(jwt.getSubject());

        applicationService.withdrawApplication(postingId, applicationId, applicantId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "내 지원 내역 조회", description = "로그인한 사용자의 전체 지원 내역을 조회합니다.")
    @GetMapping("/users/me/applications")
    public ResponseEntity<ApiResponse<List<MyApplicationListResponse>>> getMyApplications(
            @AuthenticationPrincipal Jwt jwt) {

        Long applicantId = Long.parseLong(jwt.getSubject());

        List<MyApplicationListResult> results = applicationService.getMyApplications(applicantId);

        List<MyApplicationListResponse> response = results.stream()
                .map(this::toMyListResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(application.getId(), application.getApplicationStatus());
    }

    private MyApplicationListResponse toMyListResponse(MyApplicationListResult result) {
        return new MyApplicationListResponse(
                result.applicationId(),
                result.postingId(),
                result.clubId(),
                result.clubName(),
                result.postingTitle(),
                result.applicationStatus(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
