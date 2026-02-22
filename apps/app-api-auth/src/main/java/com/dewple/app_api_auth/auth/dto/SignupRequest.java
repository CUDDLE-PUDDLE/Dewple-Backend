package com.dewple.app_api_auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 (1단계)")
public record SignupRequest(
        @Schema(description = "전화번호 본인인증 토큰", example = "vp_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
        @NotBlank(message = "본인인증 토큰은 필수입니다.")
        String verificationToken,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 20, message = "이름은 2~20자 이내여야 합니다.")
        String name,

        @Schema(description = "아이디", example = "dewple123")
        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "아이디는 영문, 숫자, 밑줄 4~20자여야 합니다.")
        String userId,

        @Schema(description = "비밀번호", example = "Password1!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다.")
        String password,

        @Schema(description = "비밀번호 확인", example = "Password1!")
        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm
) {
}
