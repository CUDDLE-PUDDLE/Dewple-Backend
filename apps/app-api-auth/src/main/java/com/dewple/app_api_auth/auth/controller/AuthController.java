package com.dewple.app_api_auth.auth.controller;

import com.dewple.app_api_auth.auth.dto.*;
import com.dewple.app_api_auth.global.config.JwtProperties;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.JwtTokenProvider;
import com.dewple.common.entity.User;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.Verification;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.service.RefreshTokenService;
import com.dewple.user.service.UserService;
import com.dewple.user.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final VerificationService verificationService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    @Operation(summary = "전화번호 인증 코드 발송", description = "SMS로 영숫자 대문자 6자리 인증 코드를 발송합니다.")
    @PostMapping("/verifications/phone")
    public ApiResponse<SendVerificationCodeResponse> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request
    ) {
        Verification verification = verificationService.sendPhoneVerificationCode(
                request.phoneNumber(), request.purpose());

        return ApiResponse.ok(new SendVerificationCodeResponse(
                verification.getPublicId().toString(),
                verification.getTokenExpireAt(),
                60
        ));
    }

    @Operation(summary = "이메일 인증 코드 발송", description = "이메일로 영숫자 대문자 6자리 인증 코드를 발송합니다.")
    @PostMapping("/verifications/email")
    public ApiResponse<SendVerificationCodeResponse> sendEmailVerificationCode(
            @Valid @RequestBody SendEmailVerificationCodeRequest request
    ) {
        Verification verification = verificationService.sendEmailVerificationCode(
                request.email(), request.purpose());

        return ApiResponse.ok(new SendVerificationCodeResponse(
                verification.getPublicId().toString(),
                verification.getTokenExpireAt(),
                60
        ));
    }

    @Operation(summary = "인증 코드 확인", description = "발송된 인증 코드를 확인하고 본인인증 토큰을 발급합니다.")
    @PostMapping("/verifications/{verificationId}/confirm")
    public ApiResponse<ConfirmVerificationCodeResponse> confirmVerificationCode(
            @PathVariable String verificationId,
            @Valid @RequestBody ConfirmVerificationCodeRequest request
    ) {
        Verification verification = verificationService.verifyPhoneCode(verificationId, request.code());

        return ApiResponse.ok(new ConfirmVerificationCodeResponse(
                verification.getToken()
        ));
    }

    @Operation(summary = "회원가입 (1단계)", description = "필수 정보를 입력하여 계정을 생성하고 JWT를 헤더로 발급합니다.")
    @PostMapping("/signup")
    public ApiResponse<Void> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response
    ) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(UserErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        User user = userService.signup(
                request.verificationToken(),
                request.name(),
                request.userId(),
                request.password()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenService.save(user, refreshToken, jwtProperties.refreshTokenExpiry());

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("Authorization-Refresh", "Bearer " + refreshToken);

        return ApiResponse.ok();
    }

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하고 JWT를 헤더로 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<Void> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        User user = userService.login(request.userId(), request.password());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenService.save(user, refreshToken, jwtProperties.refreshTokenExpiry());

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("Authorization-Refresh", "Bearer " + refreshToken);

        return ApiResponse.ok();
    }

    @Operation(summary = "로그아웃", description = "해당 사용자의 모든 리프레시 토큰을 삭제합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        refreshTokenService.deleteAllByUserId(userId);

        return ApiResponse.ok();
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 발급합니다. (RTR)")
    @PostMapping("/token/refresh")
    public ApiResponse<Void> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletResponse response
    ) {
        Jwt decodedJwt;
        try {
            decodedJwt = jwtDecoder.decode(request.refreshToken());
        } catch (JwtException e) {
            throw new BusinessException(UserErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        String tokenType = decodedJwt.getClaimAsString("type");
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(UserErrorCode.REFRESH_TOKEN_INVALID);
        }

        refreshTokenService.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException(UserErrorCode.REFRESH_TOKEN_INVALID));

        refreshTokenService.deleteByToken(request.refreshToken());

        Long userId = Long.parseLong(decodedJwt.getSubject());
        User user = userService.findById(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        refreshTokenService.save(user, newRefreshToken, jwtProperties.refreshTokenExpiry());

        response.setHeader("Authorization", "Bearer " + newAccessToken);
        response.setHeader("Authorization-Refresh", "Bearer " + newRefreshToken);

        return ApiResponse.ok();
    }

    @Operation(summary = "아이디 중복 확인", description = "사용 가능한 아이디인지 확인합니다.")
    @GetMapping("/check-userid")
    public ApiResponse<CheckUserIdResponse> checkUserId(
            @RequestParam String userId
    ) {
        boolean isAvailable = userService.isUserIdAvailable(userId);
        return ApiResponse.ok(new CheckUserIdResponse(isAvailable));
    }

    @Operation(summary = "프로필 설정 (2단계)", description = "선택 정보를 입력하여 프로필을 설정합니다.")
    @PatchMapping("/signup/profile")
    public ApiResponse<UpdateProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long id = Long.parseLong(jwt.getSubject());

        User user = userService.updateProfile(
                id,
                request.nickname(),
                request.email(),
                request.birthdate(),
                request.gender(),
                request.university(),
                request.isGraduated(),
                request.workplace()
        );

        return ApiResponse.ok(new UpdateProfileResponse(
                user.getNickname(),
                user.getEmail()
        ));
    }
}
