package com.dewple.app_api_auth.api.user.dto;

import com.dewple.user.service.PersonalFileResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "개인 자료실 파일 정보")
public record PersonalFileResponse(
        @Schema(description = "파일 ID")
        Long id,
        @Schema(description = "원본 파일명")
        String originalName,
        @Schema(description = "파일 크기 (bytes)")
        Long fileSize,
        @Schema(description = "Content-Type")
        String contentType,
        @Schema(description = "업로드 시각")
        OffsetDateTime createdAt
) {
    public static PersonalFileResponse from(PersonalFileResult result) {
        return new PersonalFileResponse(
                result.id(), result.originalName(), result.fileSize(),
                result.contentType(), result.createdAt()
        );
    }
}
