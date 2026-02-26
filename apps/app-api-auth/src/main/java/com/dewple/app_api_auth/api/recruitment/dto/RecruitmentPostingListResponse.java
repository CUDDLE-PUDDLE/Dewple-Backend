package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.common.enums.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "모집 공고 목록 응답")
public record RecruitmentPostingListResponse(

        @Schema(description = "공고 ID", example = "1")
        Long postingId,

        @Schema(description = "모집 기수", example = "1")
        Integer generationNo,

        @Schema(description = "공고 제목", example = "2026년 1기 신입 부원 모집")
        String title,

        @Schema(description = "마감일")
        OffsetDateTime endAt,

        @Schema(description = "모집 부서 목록")
        List<String> departments,

        @Schema(description = "조회수", example = "42")
        Long viewCount,

        @Schema(description = "모집 상태", example = "OPEN")
        RecruitmentStatus recruitmentStatus
) {
}
