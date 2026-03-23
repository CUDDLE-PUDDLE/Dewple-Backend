package com.dewple.user.service;

import com.dewple.common.enums.Gender;
import java.time.LocalDate;

public record SignupParam(
        String verificationToken,
        String name,
        String userId,
        String password,
        LocalDate birthdate,
        Gender gender,
        String nickname
) {
}
