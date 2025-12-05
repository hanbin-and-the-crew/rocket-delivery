package org.sparta.user.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.infrastructure.security.CustomUserDetails;
import org.sparta.user.presentation.dto.request.UserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "User API",  description = "유저 관리" )
public interface UserApiSpec {

    @Operation(
            summary = "User 회원 가입",
            description = "필수 정보를 입력받아 유저의 회원가입을 진행합니다."
    )
    @PostMapping("/signup")
    ResponseEntity<ApiResponse<Object>> signup(
            @Valid @RequestBody UserRequest.SignUpUser request,
            BindingResult bindingResult
    );

    @Operation(
            summary = "내 정보 조회",
            description = "본인의 유저 정보를 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    ApiResponse<Object> getUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo
    );

    @Operation(
            summary = "User 업데이트",
            description = "본인의 유저 정보를 수정합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    ApiResponse<Object> updateSelf(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo,
            @Valid @RequestBody UserRequest.UpdateUser request,
            BindingResult bindingResult
    );

    @Operation(
            summary = "User 탈퇴",
            description = "본인의 계정을 스스로 탈퇴합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    void deleteMe(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo
    );

    @Operation(
            summary = "ID 찾기",
            description = "본인의 이메일 정보를 바탕으로 UserName을 찾아냅니다."
    )
    @PostMapping("/id-find")
    ApiResponse<Object> findUserId(
            @Valid @RequestBody UserRequest.FindUserId request,
            BindingResult bindingResult
    );

    @Operation(
            summary = "유저 정보 조회",
            description = "특정 유저 정보를 조회합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasAnyRole('MASTER')")
    @GetMapping("/bos/{userId}")
    ApiResponse<Object> getSpecificUserInfo(
            @PathVariable UUID userId
    );

    @Operation(
            summary = "특정 User 업데이트",
            description = "특정 유저 정보를 수정합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasAnyRole('MASTER')")
    @PatchMapping("/bos/{userId}")
    ApiResponse<Object> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRequest.UpdateUser request,
            BindingResult bindingResult
    );

    @Operation(
            summary = "특정 User 탈퇴",
            description = "특정 계정을 삭제합니다.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasAnyRole('MASTER')")
    @DeleteMapping("/bos/{userId}")
    void deleteUser(
            @PathVariable UUID userId
    );

    @Operation(
            summary = "전체 회원 조회",
            description = "회원 목록을 리스트로 받습니다.")
    @PreAuthorize("hasAnyRole('MASTER')")
    @GetMapping("/bos")
    ApiResponse<Object> getAllUsers(
    );

    @Operation(
            summary = "회원가입 유저 승인",
            description = "회원가입을 넣은 유저를 승인한다.")
    @PreAuthorize("hasAnyRole('MASTER')")
    @PatchMapping("/bos/{userId}/approve")
    ApiResponse<Object> approveUser(
            @PathVariable UUID userId
    );

    @Operation(
            summary = "회원가입 유저 거절",
            description = "회원가입을 넣은 유저를 거절한다.")
    @PreAuthorize("hasAnyRole('MASTER')")
    @PatchMapping("/bos/{userId}/reject")
    ApiResponse<Object> rejectUser(
            @PathVariable UUID userId
    );
}
