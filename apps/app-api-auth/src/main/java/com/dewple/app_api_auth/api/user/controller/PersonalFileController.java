package com.dewple.app_api_auth.api.user.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.user.dto.FileDownloadResponse;
import com.dewple.app_api_auth.api.user.dto.PersonalFileResponse;
import com.dewple.app_api_auth.api.user.dto.StorageUsageResponse;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.service.PersonalFileResult;
import com.dewple.user.service.PersonalFileService;
import com.dewple.user.service.StorageUsageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "User - 개인 자료실", description = "개인 자료실 API")
@RestController
@RequestMapping("/users/me/files")
@RequiredArgsConstructor
public class PersonalFileController {

    private final PersonalFileService personalFileService;

    @Operation(summary = "파일 업로드", description = "개인 자료실에 파일을 업로드합니다. 1GB 무료 제공.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    public ApiResponse<PersonalFileResponse> uploadFile(
            @CurrentUserId Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            PersonalFileResult result = personalFileService.uploadFile(
                    userId,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    file.getInputStream()
            );
            return ApiResponse.ok(PersonalFileResponse.from(result));
        } catch (IOException e) {
            throw new BusinessException(UserErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Operation(summary = "파일 목록 조회", description = "개인 자료실의 파일 목록을 조회합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping
    public ApiResponse<List<PersonalFileResponse>> getFiles(
            @CurrentUserId Long userId
    ) {
        List<PersonalFileResponse> files = personalFileService.getFiles(userId).stream()
                .map(PersonalFileResponse::from)
                .toList();
        return ApiResponse.ok(files);
    }

    @Operation(summary = "파일 다운로드 URL 조회", description = "파일의 다운로드 URL을 생성합니다. URL은 10분간 유효합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/{fileId}/download")
    public ApiResponse<FileDownloadResponse> getDownloadUrl(
            @CurrentUserId Long userId,
            @PathVariable Long fileId
    ) {
        String url = personalFileService.getDownloadUrl(userId, fileId);
        return ApiResponse.ok(new FileDownloadResponse(url));
    }

    @Operation(summary = "파일 삭제", description = "개인 자료실의 파일을 삭제합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(
            @CurrentUserId Long userId,
            @PathVariable Long fileId
    ) {
        personalFileService.deleteFile(userId, fileId);
        return ApiResponse.ok();
    }

    @Operation(summary = "스토리지 사용량 조회", description = "개인 자료실의 스토리지 사용량을 조회합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/storage")
    public ApiResponse<StorageUsageResponse> getStorageUsage(
            @CurrentUserId Long userId
    ) {
        StorageUsageResult usage = personalFileService.getStorageUsage(userId);
        double percent = usage.totalBytes() > 0
                ? (double) usage.usedBytes() / usage.totalBytes() * 100
                : 0;
        return ApiResponse.ok(new StorageUsageResponse(usage.usedBytes(), usage.totalBytes(), Math.round(percent * 10) / 10.0));
    }
}
