package com.dewple.app_api_auth.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiry,
        long refreshTokenExpiry
) {
}
