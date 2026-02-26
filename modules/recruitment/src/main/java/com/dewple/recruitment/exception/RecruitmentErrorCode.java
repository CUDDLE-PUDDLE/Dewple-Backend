package com.dewple.recruitment.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecruitmentErrorCode implements ErrorCode {

    /**
     * 5000: Recruitment 관련 오류
     */
    GENERATION_NOT_FOUND(5001, HttpStatus.NOT_FOUND, "해당 기수를 찾을 수 없습니다."),
    DEPARTMENT_NOT_FOUND(5002, HttpStatus.NOT_FOUND, "해당 부서를 찾을 수 없습니다."),
    INVALID_INTERVIEW_SETTING(5003, HttpStatus.BAD_REQUEST, "면접 필수 설정 시 면접 일정 정보가 필요합니다."),
    INVALID_DATE_RANGE(5004, HttpStatus.BAD_REQUEST, "종료일은 시작일 이후여야 합니다."),
    APPLICATION_FORM_SERIALIZE_ERROR(5005, HttpStatus.INTERNAL_SERVER_ERROR, "지원서 양식 저장 중 오류가 발생하였습니다."),
    NOT_POSTING_CREATOR(5006, HttpStatus.FORBIDDEN, "해당 공고의 생성자만 수정/삭제할 수 있습니다."),
    POSTING_NOT_FOUND(5007, HttpStatus.NOT_FOUND, "해당 모집 공고를 찾을 수 없습니다."),
    POSTING_NOT_DRAFT(5008, HttpStatus.BAD_REQUEST, "임시 저장은 초안 상태의 공고만 가능합니다."),
    FORM_COMPONENT_REMOVAL_NOT_ALLOWED(5009, HttpStatus.BAD_REQUEST, "지원서 양식에서 기존 컴포넌트를 삭제할 수 없습니다."),
    POSTING_NOT_OPEN(5010, HttpStatus.BAD_REQUEST, "게시된 상태의 공고만 수정할 수 있습니다."),
    POSTING_ALREADY_CLOSED(5011, HttpStatus.BAD_REQUEST, "이미 마감된 공고입니다."),
    POSTING_NOT_EXPIRED(5012, HttpStatus.BAD_REQUEST, "마감 기한이 지난 공고는 조기 마감할 수 없습니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
