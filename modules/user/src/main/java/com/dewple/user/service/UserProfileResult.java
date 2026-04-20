package com.dewple.user.service;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileResult(
        String name,
        String profileImg,
        String selfIntroduction,
        String mbti,
        List<String> interests,
        BigDecimal star
) {
}
