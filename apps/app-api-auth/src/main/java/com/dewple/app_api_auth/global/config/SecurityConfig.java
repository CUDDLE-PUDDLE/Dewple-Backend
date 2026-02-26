package com.dewple.app_api_auth.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    // Auth APIs (인증 불필요)
                    "/auth/verifications/**",
                    "/auth/signup",
                    "/auth/login",
                    "/auth/token/refresh",
                    // User APIs (인증 불필요)
                    "/users/check-userid",
                    // Recruitment APIs (인증 불필요)
                    "/recruitment-posts/*/view"
                ).permitAll()
                // previous-form은 인증 필요 (permitAll보다 먼저 매칭)
                .requestMatchers(HttpMethod.GET, "/clubs/*/recruitment-posts/previous-form").authenticated()
                // 공개 조회 API (GET만 허용)
                .requestMatchers(HttpMethod.GET, "/clubs/*/recruitment-posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/clubs/*/recruitment-posts/*").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
