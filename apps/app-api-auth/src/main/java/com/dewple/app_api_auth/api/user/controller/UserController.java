package com.dewple.app_api_auth.api.user.controller;

import com.dewple.app_api_auth.api.user.dto.CheckUserIdResponse;
import com.dewple.app_api_auth.api.user.dto.UpdateProfileRequest;
import com.dewple.app_api_auth.api.user.dto.UpdateProfileResponse;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.common.entity.User;
import com.dewple.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "아이디 중복 확인", description = "사용 가능한 아이디인지 확인합니다.")
    @GetMapping("/check-userid")
    public ApiResponse<CheckUserIdResponse> checkUserId(
            @RequestParam String userId
    ) {
        boolean isAvailable = userService.isUserIdAvailable(userId);
        return ApiResponse.ok(new CheckUserIdResponse(isAvailable));
    }

    @Operation(summary = "프로필 설정", description = "선택 정보를 입력하여 프로필을 설정합니다.")
    @PatchMapping("/me/profile")
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
