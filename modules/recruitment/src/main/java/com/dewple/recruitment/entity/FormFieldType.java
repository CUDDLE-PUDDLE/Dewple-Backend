package com.dewple.recruitment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum FormFieldType {

    TEXTAREA("textarea"),
    CHOICE("choice"),
    FILE("file"),
    CALENDAR("calendar"),
    WHEN2MEET("when2meet"),
    ;

    private final String fieldName;

    public static List<String> allFieldNames() {
        return List.of(values()).stream()
                .map(FormFieldType::getFieldName)
                .toList();
    }
}
