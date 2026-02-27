package com.dewple.user.service;

import com.dewple.common.enums.Gender;
import com.dewple.common.enums.Mbti;
import com.dewple.common.enums.University;

import java.time.LocalDate;
import java.util.List;

public record EditMyProfileCommand(
        String nickname,
        String email,
        LocalDate birthdate,
        Gender gender,
        University university,
        Boolean isGraduated,
        String workplace,
        String profileImg,
        String selfIntroduction,
        Mbti mbti,
        List<Long> categoryIds
) {
}
