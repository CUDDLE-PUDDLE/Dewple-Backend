package com.dewple.app_api_auth.api.recruitment.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Schema(description = "비회원 지원서 제출 요청")
public record GuestApplicationRequest(
        @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
        @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다.")
        @Schema(description = "지원자 전화번호", example = "01012345678")
        String guestPhone,

        @NotNull(message = "답변은 필수 입력 항목입니다.")
        @Schema(description = "지원서 답변 목록")
        List<Object> answers
) {
    public String toAnswersJson(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(answers);
    }
}
