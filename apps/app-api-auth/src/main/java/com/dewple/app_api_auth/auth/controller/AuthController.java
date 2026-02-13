package com.dewple.app_api_auth.auth.controller;

import com.dewple.app_api_auth.auth.dto.ConfirmVerificationCodeRequest;
import com.dewple.app_api_auth.auth.dto.ConfirmVerificationCodeResponse;
import com.dewple.app_api_auth.auth.dto.SendVerificationCodeRequest;
import com.dewple.app_api_auth.auth.dto.SendVerificationCodeResponse;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.user.entity.Verification;
import com.dewple.user.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final VerificationService verificationService;

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

    @Operation(summary = "인증 코드 확인", description = "발송된 인증 코드를 확인하고 인증 토큰을 발급합니다.")
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
}
