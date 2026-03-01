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
    INVALID_STATUS_FILTER(5013, HttpStatus.BAD_REQUEST, "상태 필터는 OPEN 또는 CLOSED만 가능합니다."),

    /**
     * 5100: Application 관련 오류
     */
    APPLICATION_NOT_FOUND(5100, HttpStatus.NOT_FOUND, "해당 지원서를 찾을 수 없습니다."),
    APPLICATION_ALREADY_SUBMITTED(5101, HttpStatus.CONFLICT, "이미 해당 공고에 제출된 지원서가 존재합니다."),
    APPLICATION_NOT_WITHDRAWABLE(5102, HttpStatus.BAD_REQUEST, "합격 또는 불합격 상태의 지원서는 철회할 수 없습니다."),
    POSTING_NOT_ACCEPTING(5103, HttpStatus.BAD_REQUEST, "현재 지원을 받지 않는 공고입니다."),
    APPLICATION_SCHEMA_NOT_FOUND(5104, HttpStatus.NOT_FOUND, "해당 공고의 지원서 양식을 찾을 수 없습니다."),
    APPLICATION_NOT_OWNER(5105, HttpStatus.FORBIDDEN, "본인의 지원서만 관리할 수 있습니다."),
    APPLICATION_NOT_EDITABLE(5106, HttpStatus.BAD_REQUEST, "수정할 수 없는 상태의 지원서입니다."),
    APPLICATION_EDIT_WINDOW_CLOSED(5107, HttpStatus.BAD_REQUEST, "지원서 수정 기간이 종료되었습니다."),
    GUEST_APPLICATION_ALREADY_SUBMITTED(5108, HttpStatus.CONFLICT, "해당 전화번호로 이미 제출된 지원서가 존재합니다."),
    GUEST_APPLICATION_NOT_FOUND(5109, HttpStatus.NOT_FOUND, "해당 전화번호로 제출된 지원서를 찾을 수 없습니다."),
    APPLICATION_SOME_NOT_FOUND(5110, HttpStatus.BAD_REQUEST, "일부 지원서를 찾을 수 없습니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
