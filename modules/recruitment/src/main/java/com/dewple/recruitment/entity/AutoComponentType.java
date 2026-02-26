package com.dewple.recruitment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AutoComponentType {

    DEPARTMENT_SELECT("__auto_department_select_"),
    INTERVIEW_SCHEDULE("__auto_interview_schedule_"),
    ;

    private final String keyPrefix;
}
