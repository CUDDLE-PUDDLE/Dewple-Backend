package com.dewple.app_api_auth.api.recruitment.controller;

import com.dewple.app_api_auth.api.recruitment.dto.ApplicationDetailResponse;
import com.dewple.app_api_auth.api.recruitment.dto.ApplicationListResponse;
import com.dewple.app_api_auth.api.recruitment.dto.BatchChangeApplicationStatusRequest;
import com.dewple.app_api_auth.api.recruitment.dto.ChangeApplicationStatusRequest;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.common.enums.ApplicationStatus;
import com.dewple.recruitment.service.ApplicationDetailResult;
import com.dewple.recruitment.service.ApplicationListResult;
import com.dewple.recruitment.service.ApplicationManageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recruitment - 지원서 관리", description = "운영진 지원서 관리 API")
@RestController
@RequiredArgsConstructor
public class ApplicationManageController {

    private final ApplicationManageService applicationManageService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "지원자 목록 조회", description = "특정 공고의 지원자 목록을 페이징으로 조회합니다. 상태 필터 및 이름/전화번호 검색을 지원합니다.")
    @GetMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications")
    public ResponseEntity<ApiResponse<ApiResponse.PageResult<ApplicationListResponse>>> getApplicationList(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        Long userId = Long.parseLong(jwt.getSubject());

        Page<ApplicationListResult> results = applicationManageService.getApplicationList(
                clubId, userId, postingId, status, keyword, pageable);

        Page<ApplicationListResponse> responsePage = results.map(r -> new ApplicationListResponse(
                r.applicationId(), r.applicantName(), r.applicantPhone(),
                r.appliedAt(), r.applicationStatus()));

        return ResponseEntity.ok(ApiResponse.ok(responsePage));
    }

    @Operation(summary = "지원자 상세 조회", description = "특정 지원서의 상세 정보를 조회합니다.")
    @GetMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getApplicationDetail(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) throws JsonProcessingException {

        Long userId = Long.parseLong(jwt.getSubject());

        ApplicationDetailResult result = applicationManageService.getApplicationDetail(
                clubId, userId, postingId, applicationId);

        Object answers = objectMapper.readValue(result.answers(), Object.class);

        ApplicationDetailResponse response = new ApplicationDetailResponse(
                result.applicationId(), result.applicantName(), result.applicantPhone(),
                answers, result.appliedAt(), result.applicationStatus());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "지원서 상태 변경", description = "특정 지원서의 합격/불합격 상태를 변경합니다.")
    @PatchMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications/{applicationId}/status")
    public ResponseEntity<ApiResponse<Void>> changeApplicationStatus(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeApplicationStatusRequest request) {

        Long userId = Long.parseLong(jwt.getSubject());

        applicationManageService.changeApplicationStatus(
                clubId, userId, postingId, applicationId, request.applicationStatus());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "지원서 일괄 상태 변경", description = "여러 지원서의 합격/불합격 상태를 일괄 변경합니다.")
    @PatchMapping("/clubs/{clubId}/recruitment-posts/{postingId}/applications/status")
    public ResponseEntity<ApiResponse<Void>> batchChangeApplicationStatus(
            @PathVariable Long clubId,
            @PathVariable Long postingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BatchChangeApplicationStatusRequest request) {

        Long userId = Long.parseLong(jwt.getSubject());

        applicationManageService.batchChangeApplicationStatus(
                clubId, userId, postingId, request.applicationIds(), request.applicationStatus());

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
