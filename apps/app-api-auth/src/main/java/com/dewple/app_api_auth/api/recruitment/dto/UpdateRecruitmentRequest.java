package com.dewple.app_api_auth.api.recruitment.dto;

import com.dewple.recruitment.service.RecruitmentService.UpdateRecruitmentCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateRecruitmentRequest(

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotEmpty(message = "공고 본문 컴포넌트는 최소 1개 이상이어야 합니다.")
        @Valid
        List<PostingContentComponent> content,

        @NotNull(message = "지원서 양식은 필수입니다.")
        @Valid
        ApplicationFormRequest applicationForm
) {

    public UpdateRecruitmentCommand toCommand(ObjectMapper objectMapper) throws JsonProcessingException {
        String contentJson = objectMapper.writeValueAsString(content);
        String applicationFormJson = objectMapper.writeValueAsString(applicationForm);
        return new UpdateRecruitmentCommand(title, contentJson, applicationFormJson);
    }
}
