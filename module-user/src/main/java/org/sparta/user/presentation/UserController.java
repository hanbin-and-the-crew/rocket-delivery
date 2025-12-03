package org.sparta.user.presentation;

import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.common.error.BusinessException;
import org.sparta.user.application.service.UserService;
import org.sparta.user.domain.enums.UserStatusEnum;
import org.sparta.user.domain.error.UserErrorType;
import org.sparta.user.infrastructure.security.CustomUserDetails;
import org.sparta.user.presentation.dto.request.UserRequest;
import org.sparta.user.presentation.dto.response.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController implements UserApiSpec {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/health")
    public String hello() {
        return "User OK";
    }

    @Override
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Object>> signup(@Valid @RequestBody UserRequest.SignUpUser request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 여러 validation 메시지를 모아서 전달
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ResponseEntity // 실패 시에는 ResponseEntity로 감싸주지 않으면 200 OK로 나가기 때문에 wrapping 과정 필요
                    .badRequest()
                    .body(ApiResponse.fail("VALIDATION_FAILED", errorMessage));
        }

        UserResponse.SignUpUser response = userService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping("/me")
    public ApiResponse<Object> getUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo
    ) {
        UserResponse.GetUser response = userService.getUserInfo(userDetailsInfo);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/me")
    public ApiResponse<Object> updateSelf(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo, // Swagger 문서엔 숨기고 싶다면 @Parameter(hidden = true)
            @Valid @RequestBody UserRequest.UpdateUser request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ApiResponse.fail("VALIDATION_FAILED", "회원 수정 실패");
        }
        if (!passwordEncoder.matches(request.oldPassword(), userDetailsInfo.getPassword())) {
            throw new BusinessException(UserErrorType.UNAUTHORIZED, "현재 비밀번호를 확인해주세요.");
        }
        // TODO: 서비스 호출 후 결과 리턴
        UserResponse.UpdateUser response = userService.updateSelf(userDetailsInfo, request);
        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/me")
    public void deleteMe(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo
    ) {
        userService.deleteSelf(userDetailsInfo);
    }

    @Override
    @PostMapping("/id-find")
    public ApiResponse<Object> findUserId(
            @Valid @RequestBody UserRequest.FindUserId request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ApiResponse.fail("VALIDATION_FAILED", errorMessage);
        }

        UserResponse.FindUserId response = userService.findUserId(request);
        return ApiResponse.success(response);
    }

    /**
     * BOS 관점
     */
    @Override
    @GetMapping("/bos/{userId}")
    public ApiResponse<Object> getSpecificUserInfo(
            @PathVariable UUID userId
    ) {
        UserResponse.GetUser response = userService.getSpecificUserInfo(userId);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/bos/{userId}")
    public ApiResponse<Object> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRequest.UpdateUser request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ApiResponse.fail("VALIDATION_FAILED", "회원 수정 실패");
        }
        /* 운영자는 현재 비밀번호를 알 필요 없으니
        if (!passwordEncoder.matches(request.oldPassword(), userDetailsInfo.getPassword())) {
            throw new BusinessException(UserErrorType.UNAUTHORIZED, "현재 비밀번호를 확인해주세요.");
        }*/
        // TODO: 서비스 호출 후 결과 리턴
        UserResponse.UpdateUser response = userService.updateUser(userId, request);
        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/bos/{userId}")
    public void deleteUser(
            @PathVariable UUID userId
    ) {
        userService.deleteUser(userId);
    }

    @Override
    @GetMapping("/bos")
    public ApiResponse<Object> getAllUsers() {
        List<UserResponse.GetUser> response = userService.getAllUsers();
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/bos/{userId}/approve")
    public ApiResponse<Object> approveUser(@PathVariable UUID userId) {
        userService.updateUserStatus(userId, UserStatusEnum.APPROVE);
        return ApiResponse.success("회원가입이 승인되었습니다.");
    }

    @Override
    @PatchMapping("/bos/{userId}/reject")
    public ApiResponse<Object> rejectUser(@PathVariable UUID userId) {
        userService.updateUserStatus(userId, UserStatusEnum.REJECTED);
        return ApiResponse.success("회원가입이 거절되었습니다.");
    }
}
