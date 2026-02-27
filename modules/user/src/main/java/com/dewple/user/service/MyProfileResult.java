package com.dewple.user.service;

import com.dewple.common.entity.User;

import java.util.List;

public record MyProfileResult(
        User user,
        List<String> interests
) {
}
