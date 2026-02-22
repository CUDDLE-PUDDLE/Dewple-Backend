package com.dewple.app_api_auth.auth.controller;

import com.dewple.app_api_auth.auth.dto.*;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.JwtTokenProvider;
import com.dewple.common.entity.User;
import com.dewple.common.exception.BusinessException;
import com.dewple.user.entity.Verification;
import com.dewple.user.exception.UserErrorCode;
import com.dewple.user.service.UserService;
import com.dewple.user.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final VerificationService verificationService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

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
    public ApiResponse<SignupResponse> signup(
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

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("Authorization-Refresh", "Bearer " + refreshToken);

        return ApiResponse.ok(new SignupResponse(user.getUserId()));
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
        Long userId = Long.parseLong(jwt.getSubject());

        User user = userService.updateProfile(
                userId,
                request.nickname(),
                request.email(),
                request.birthdate(),
                request.gender(),
                request.university(),
                request.isGraduated(),
                request.workplace()
        );

        return ApiResponse.ok(new UpdateProfileResponse(
                user.getUserId(),
                user.getNickname(),
                user.getEmail()
        ));
    }
}
