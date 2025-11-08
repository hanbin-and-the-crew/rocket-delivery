package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.infrastructure.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User API",  description = "유저 관리" )
public interface UserApiSpec {

    @Operation(
            summary = "User 회원 가입",
            description = "필수 정보를 입력받아 유저의 회원가입을 진행합니다."
    )
    @PostMapping("/signup")
    ApiResponse<Object> signup(
            @Valid @RequestBody UserRequest.SignUpUser requestDto,
            BindingResult bindingResult
    );

    @Operation(
            summary = "User 업데이트",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    ApiResponse<Object> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo,
            @Valid @RequestBody UserRequest.UpdateUser request,
            BindingResult bindingResult
    );

    @Operation(
            summary = "User 탈퇴",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    void deleteMe(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo
    );

}
