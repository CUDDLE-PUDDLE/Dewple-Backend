package com.dewple.user.service;

public record LoginParam(
        String userId,
        String rawPassword
) {
}
