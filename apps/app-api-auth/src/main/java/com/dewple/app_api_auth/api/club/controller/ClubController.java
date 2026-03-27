package com.dewple.app_api_auth.api.club.controller;

import static com.dewple.app_api_auth.global.config.SwaggerConfig.BEARER_AUTH;

import com.dewple.app_api_auth.api.club.dto.CreateClubRequest;
import com.dewple.app_api_auth.api.club.dto.CreateClubResponse;
import com.dewple.app_api_auth.global.response.ApiResponse;
import com.dewple.app_api_auth.global.security.CurrentUserId;
import com.dewple.club.service.ClubService;
import com.dewple.club.service.CreateClubParam;
import com.dewple.club.service.CreateClubResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Club", description = "동아리 API")
@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @Operation(summary = "동아리 생성", description = "동아리를 생성합니다. 생성자가 자동으로 회장이 됩니다. 회장 동시 운영 최대 5개.")
    @SecurityRequirement(name = BEARER_AUTH)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateClubResponse> createClub(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateClubRequest request
    ) {
        CreateClubParam param = new CreateClubParam(
                request.name(), request.isVerificationRequired(),
                request.activityType(), request.foundedDate(),
                request.categoryIds(), request.regionIds()
        );

        CreateClubResult result = clubService.createClub(userId, param);
        return ApiResponse.ok(CreateClubResponse.from(result));
    }
}
