package com.dewple.app_api_auth.api.user.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.user.dto.CheckUserIdResponse;
import com.dewple.app_api_auth.api.user.dto.EditMyProfileRequest;
import com.dewple.app_api_auth.api.user.dto.GetMyProfileResponse;
import com.dewple.app_api_auth.api.user.dto.UpdateProfileRequest;
import com.dewple.app_api_auth.api.user.dto.UpdateProfileResponse;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.common.entity.User;
import com.dewple.user.service.EditMyProfileParam;
import com.dewple.user.service.MyProfileResult;
import com.dewple.user.service.UpdateProfileParam;
import com.dewple.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자가 본인의 프로필 정보를 조회합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @GetMapping("/me")
    public ApiResponse<GetMyProfileResponse> getMyProfile(
            @CurrentUserId Long userId
    ) {
        MyProfileResult profile = userService.getMyProfile(userId);
        User user = profile.user();

        return ApiResponse.ok(new GetMyProfileResponse(
                user.getName(),
                user.getProfileImg(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getBirthdate(),
                user.getGender(),
                user.getUniversity(),
                user.getIsGraduated(),
                user.getWorkplace(),
                user.getSelfIntroduction(),
                user.getMbti(),
                profile.interests()
        ));
    }

    @Operation(summary = "내 프로필 수정", description = "로그인한 사용자가 본인의 프로필을 수정합니다. 전달된 필드만 변경됩니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/me")
    public ApiResponse<GetMyProfileResponse> editMyProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody EditMyProfileRequest request
    ) {
        EditMyProfileParam param = new EditMyProfileParam(
                request.nickname(), request.email(), request.birthdate(),
                request.gender(), request.university(), request.isGraduated(),
                request.workplace(), request.profileImg(), request.selfIntroduction(),
                request.mbti(), request.categoryIds()
        );

        MyProfileResult profile = userService.editMyProfile(userId, param);
        User user = profile.user();

        return ApiResponse.ok(new GetMyProfileResponse(
                user.getName(),
                user.getProfileImg(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getBirthdate(),
                user.getGender(),
                user.getUniversity(),
                user.getIsGraduated(),
                user.getWorkplace(),
                user.getSelfIntroduction(),
                user.getMbti(),
                profile.interests()
        ));
    }

    @Operation(summary = "아이디 중복 확인", description = "사용 가능한 아이디인지 확인합니다.")
    @GetMapping("/check-userid")
    public ApiResponse<CheckUserIdResponse> checkUserId(
            @RequestParam String userId
    ) {
        boolean isAvailable = userService.isUserIdAvailable(userId);
        return ApiResponse.ok(new CheckUserIdResponse(isAvailable));
    }

    @Operation(summary = "회원가입 시 프로필 설정", description = "선택 정보를 입력하여 프로필을 설정합니다.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PatchMapping("/me/profile")
    public ApiResponse<UpdateProfileResponse> updateProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UpdateProfileParam param = new UpdateProfileParam(
                request.nickname(), request.email(), request.birthdate(),
                request.gender(), request.university(), request.isGraduated(),
                request.workplace()
        );

        User user = userService.updateProfile(userId, param);

        return ApiResponse.ok(new UpdateProfileResponse(
                user.getNickname(),
                user.getEmail()
        ));
    }
}
