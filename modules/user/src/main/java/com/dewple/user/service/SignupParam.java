package com.dewple.user.service;

public record SignupParam(
        String verificationToken,
        String name,
        String userId,
        String password
) {
}
