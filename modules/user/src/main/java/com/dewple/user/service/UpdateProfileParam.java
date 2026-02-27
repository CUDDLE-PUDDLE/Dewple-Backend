package com.dewple.user.service;

import com.dewple.common.enums.Gender;
import com.dewple.common.enums.University;

import java.time.LocalDate;

public record UpdateProfileParam(
        String nickname,
        String email,
        LocalDate birthdate,
        Gender gender,
        University university,
        Boolean isGraduated,
        String workplace
) {
}
