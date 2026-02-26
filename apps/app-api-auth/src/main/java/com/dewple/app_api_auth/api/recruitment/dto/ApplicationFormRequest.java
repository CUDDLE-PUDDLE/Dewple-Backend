package com.dewple.app_api_auth.api.recruitment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ApplicationFormRequest(

        @Valid
        List<TextareaField> textarea,

        @Valid
        List<ChoiceField> choice,

        @Valid
        List<FileField> file,

        @Valid
        List<CalendarField> calendar,

        @Valid
        List<When2MeetField> when2meet
) {

    public record TextareaField(

            @NotNull(message = "정렬 순서는 필수입니다.")
            Integer orderNumber,

            @NotBlank(message = "컴포넌트 키는 필수입니다.")
            String key,

            @NotBlank(message = "질문 내용은 필수입니다.")
            String question,

            @NotNull(message = "필수 여부는 필수입니다.")
            Boolean required,

            Integer maxLength
    ) {
    }

    public record ChoiceField(

            @NotNull(message = "정렬 순서는 필수입니다.")
            Integer orderNumber,

            @NotBlank(message = "컴포넌트 키는 필수입니다.")
            String key,

            @NotBlank(message = "질문 내용은 필수입니다.")
            String question,

            @NotEmpty(message = "선택지는 최소 1개 이상이어야 합니다.")
            List<String> options,

            @NotNull(message = "기타 선택지 허용 여부는 필수입니다.")
            Boolean others,

            Integer maxOthersLength,

            @NotNull(message = "필수 여부는 필수입니다.")
            Boolean required
    ) {
    }

    public record FileField(

            @NotNull(message = "정렬 순서는 필수입니다.")
            Integer orderNumber,

            @NotBlank(message = "컴포넌트 키는 필수입니다.")
            String key,

            @NotBlank(message = "질문 내용은 필수입니다.")
            String question,

            String uploadUrl,

            List<String> allowType,

            @NotNull(message = "필수 여부는 필수입니다.")
            Boolean required,

            Integer maxSize
    ) {
    }

    public record CalendarField(

            @NotNull(message = "정렬 순서는 필수입니다.")
            Integer orderNumber,

            @NotBlank(message = "컴포넌트 키는 필수입니다.")
            String key,

            @NotBlank(message = "질문 내용은 필수입니다.")
            String question,

            Integer maxSelect,

            @NotNull(message = "기간 선택 여부는 필수입니다.")
            Boolean period,

            String timeZone,

            @NotNull(message = "시작일은 필수입니다.")
            LocalDate startDate,

            @NotNull(message = "종료일은 필수입니다.")
            LocalDate endDate,

            @NotNull(message = "필수 여부는 필수입니다.")
            Boolean required
    ) {
    }

    public record When2MeetField(

            @NotNull(message = "정렬 순서는 필수입니다.")
            Integer orderNumber,

            @NotBlank(message = "컴포넌트 키는 필수입니다.")
            String key,

            @NotBlank(message = "질문 내용은 필수입니다.")
            String question,

            Integer maxSelect,

            String timeZone,

            @NotNull(message = "시작 시간은 필수입니다.")
            LocalTime startTime,

            @NotNull(message = "종료 시간은 필수입니다.")
            LocalTime endTime,

            @NotNull(message = "시작일은 필수입니다.")
            LocalDate startDate,

            @NotNull(message = "종료일은 필수입니다.")
            LocalDate endDate,

            @NotNull(message = "필수 여부는 필수입니다.")
            Boolean required
    ) {
    }
}
