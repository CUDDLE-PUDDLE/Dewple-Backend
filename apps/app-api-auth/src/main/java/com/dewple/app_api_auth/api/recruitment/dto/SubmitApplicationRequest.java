package com.dewple.app_api_auth.api.recruitment.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "지원서 제출/임시저장 요청")
public record SubmitApplicationRequest(
        @NotNull(message = "답변은 필수 입력 항목입니다.")
        @Schema(description = "지원서 답변 목록")
        List<Object> answers
) {
    public String toAnswersJson(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(answers);
    }
}
