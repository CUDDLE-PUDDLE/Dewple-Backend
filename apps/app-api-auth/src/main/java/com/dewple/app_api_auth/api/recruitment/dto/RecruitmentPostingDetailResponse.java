package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.common.enums.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "모집 공고 상세 응답")
public record RecruitmentPostingDetailResponse(

        @Schema(description = "공고 ID", example = "1")
        Long postingId,

        @Schema(description = "동아리 ID", example = "1")
        Long clubId,

        @Schema(description = "동아리 이름", example = "듀플")
        String clubName,

        @Schema(description = "모집 기수", example = "1")
        Integer generationNo,

        @Schema(description = "공고 제목", example = "2026년 1기 신입 부원 모집")
        String title,

        @Schema(description = "공고 본문 (JSON)")
        String content,

        @Schema(description = "테마 색상", example = "#FF5733")
        String themeColor,

        @Schema(description = "총 모집 인원", example = "10")
        Integer capacity,

        @Schema(description = "모집 시작일")
        OffsetDateTime startAt,

        @Schema(description = "마감일")
        OffsetDateTime endAt,

        @Schema(description = "결과 발표일")
        LocalDate resultDate,

        @Schema(description = "활동 종료일")
        LocalDate endOfGenerationDate,

        @Schema(description = "2차 면접 여부")
        Boolean hasSecondInterview,

        @Schema(description = "모집 부서 목록")
        List<DepartmentInfo> departments,

        @Schema(description = "모집 절차 목록")
        List<ProcessInfo> processes,

        @Schema(description = "지원서 양식 (JSON)")
        String applicationForm,

        @Schema(description = "조회수", example = "42")
        Long viewCount,

        @Schema(description = "모집 상태", example = "OPEN")
        RecruitmentStatus recruitmentStatus
) {

    @Schema(description = "모집 부서 정보")
    public record DepartmentInfo(
            @Schema(description = "부서 이름", example = "개발팀")
            String name,

            @Schema(description = "모집 인원", example = "5")
            Integer count
    ) {
    }

    @Schema(description = "모집 절차 정보")
    public record ProcessInfo(
            @Schema(description = "절차 순서", example = "1")
            Integer processOrder,

            @Schema(description = "절차 이름", example = "서류 접수")
            String name,

            @Schema(description = "절차 유형", example = "DOCUMENT")
            String processType,

            @Schema(description = "시작일")
            OffsetDateTime startAt,

            @Schema(description = "종료일")
            OffsetDateTime endAt
    ) {
    }
}
