package com.dewple.recruitment.exception;

import com.dewple.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecruitmentErrorCode implements ErrorCode {

    // 5100 : 모집 공고 관련 오류
    GENERATION_NOT_FOUND(5101, HttpStatus.NOT_FOUND, "해당 기수를 찾을 수 없습니다."),
    DEPARTMENT_NOT_FOUND(5102, HttpStatus.NOT_FOUND, "해당 부서를 찾을 수 없습니다."),
    INVALID_INTERVIEW_SETTING(5103, HttpStatus.BAD_REQUEST, "면접 필수 설정 시 면접 일정 정보가 필요합니다."),
    INVALID_DATE_RANGE(5104, HttpStatus.BAD_REQUEST, "종료일은 시작일 이후여야 합니다."),
    APPLICATION_FORM_SERIALIZE_ERROR(5105, HttpStatus.INTERNAL_SERVER_ERROR, "지원서 양식 저장 중 오류가 발생하였습니다."),
    NOT_POSTING_CREATOR(5106, HttpStatus.FORBIDDEN, "해당 공고의 생성자만 수정/삭제할 수 있습니다."),
    POSTING_NOT_FOUND(5107, HttpStatus.NOT_FOUND, "해당 모집 공고를 찾을 수 없습니다."),
    POSTING_NOT_DRAFT(5108, HttpStatus.BAD_REQUEST, "임시 저장은 초안 상태의 공고만 가능합니다."),
    POSTING_NOT_OPEN(5110, HttpStatus.BAD_REQUEST, "게시된 상태의 공고만 수정할 수 있습니다."),
    POSTING_ALREADY_CLOSED(5111, HttpStatus.BAD_REQUEST, "이미 마감된 공고입니다."),
    POSTING_NOT_EXPIRED(5112, HttpStatus.BAD_REQUEST, "마감 기한이 지난 공고는 조기 마감할 수 없습니다."),
    INVALID_STATUS_FILTER(5113, HttpStatus.BAD_REQUEST, "상태 필터는 OPEN 또는 CLOSED만 가능합니다."),
    DEADLINE_CHANGE_LIMIT_EXCEEDED(5114, HttpStatus.BAD_REQUEST, "마감일 변경은 최대 2회까지 가능합니다."),
    COMPONENT_MODIFICATION_NOT_ALLOWED(5117, HttpStatus.BAD_REQUEST, "기존 지원서 컴포넌트는 수정할 수 없습니다. 추가 또는 삭제만 가능합니다."),
    WAITLISTED_APPLICANTS_EXIST(5115, HttpStatus.BAD_REQUEST, "합격예비 지원자가 존재합니다. 추가합격 기간을 설정해주세요."),
    INVALID_ADDITIONAL_ACCEPTANCE_PERIOD(5116, HttpStatus.BAD_REQUEST, "추가합격 기간은 1일 이상 14일 이하여야 합니다."),

    // 5200: 지원서 관련 오류
    APPLICATION_NOT_FOUND(5201, HttpStatus.NOT_FOUND, "해당 지원서를 찾을 수 없습니다."),
    APPLICATION_ALREADY_SUBMITTED(5202, HttpStatus.CONFLICT, "이미 해당 공고에 제출된 지원서가 존재합니다."),
    APPLICATION_NOT_WITHDRAWABLE(5203, HttpStatus.BAD_REQUEST, "합격/불합격/합격예비 상태의 지원서는 철회할 수 없습니다."),
    POSTING_NOT_ACCEPTING(5204, HttpStatus.BAD_REQUEST, "현재 지원을 받지 않는 공고입니다."),
    APPLICATION_SCHEMA_NOT_FOUND(5205, HttpStatus.NOT_FOUND, "해당 공고의 지원서 양식을 찾을 수 없습니다."),
    APPLICATION_NOT_OWNER(5206, HttpStatus.FORBIDDEN, "본인의 지원서만 관리할 수 있습니다."),
    APPLICATION_NOT_EDITABLE(5207, HttpStatus.BAD_REQUEST, "수정할 수 없는 상태의 지원서입니다."),
    APPLICATION_EDIT_WINDOW_CLOSED(5208, HttpStatus.BAD_REQUEST, "지원서 수정 기간이 종료되었습니다."),
    INVALID_STATUS_TRANSITION(5209, HttpStatus.BAD_REQUEST, "허용되지 않는 상태 변경입니다."),
    APPLICATION_SOME_NOT_FOUND(5211, HttpStatus.BAD_REQUEST, "일부 지원서를 찾을 수 없습니다."),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
