package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.recruitment.service.RecruitmentService.CreateRecruitmentCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateRecruitmentRequest(

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        Integer generation,

        @NotEmpty(message = "공고 본문 컴포넌트는 최소 1개 이상이어야 합니다.")
        @Valid
        List<PostingContentComponent> content,

        @NotEmpty(message = "모집 부서는 최소 1개 이상이어야 합니다.")
        @Valid
        List<DepartmentRequest> departments,

        @NotNull(message = "모집 시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "모집 마감일은 필수입니다.")
        LocalDate endDate,

        @NotNull(message = "결과 발표일은 필수입니다.")
        LocalDate resultDate,

        @NotNull(message = "기수 활동 종료일은 필수입니다.")
        LocalDate endOfGenerationDate,

        @NotNull(message = "2차 면접 여부는 필수입니다.")
        Boolean hasSecondInterview,

        LocalDate interviewStartDate,

        LocalDate interviewEndDate,

        LocalTime interviewStartTime,

        LocalTime interviewEndTime,

        @NotNull(message = "지원서 양식은 필수입니다.")
        @Valid
        ApplicationFormRequest applicationForm
) {

        public record DepartmentRequest(

                @NotNull(message = "부서 ID는 필수입니다.")
                Long id,

                @NotNull(message = "모집 인원은 필수입니다.")
                Integer count
        ) {
        }

        public CreateRecruitmentCommand toCommand(ObjectMapper objectMapper) throws JsonProcessingException {
                List<CreateRecruitmentCommand.DepartmentInfo> deptInfos = departments.stream()
                        .map(dept -> new CreateRecruitmentCommand.DepartmentInfo(dept.id(), dept.count()))
                        .toList();

                String contentJson = objectMapper.writeValueAsString(content);
                String applicationFormJson = objectMapper.writeValueAsString(applicationForm);

                return new CreateRecruitmentCommand(
                        title,
                        generation,
                        contentJson,
                        deptInfos,
                        startDate,
                        endDate,
                        resultDate,
                        endOfGenerationDate,
                        hasSecondInterview,
                        interviewStartDate,
                        interviewEndDate,
                        interviewStartTime,
                        interviewEndTime,
                        applicationFormJson
                );
        }
}
