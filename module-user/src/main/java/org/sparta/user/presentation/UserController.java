package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.apache.tomcat.util.http.ResponseUtil;
import org.sparta.common.api.ApiResponse;
import org.sparta.common.error.BusinessException;
import org.sparta.user.application.service.UserService;
import org.sparta.user.domain.error.UserErrorType;
import org.sparta.user.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController implements UserApiSpec{

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @PostMapping("/signup")
    public ApiResponse<Object> signup(@Valid @RequestBody UserRequest.SignUpUser requestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 여러 validation 메시지를 모아서 전달
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ApiResponse.fail("VALIDATION_FAILED", errorMessage);
        }

        UserResponse.SignUpUser responseDto = userService.signup(requestDto);
        return ApiResponse.success(responseDto);
    }

    @PatchMapping("/me")
    public ApiResponse<Object> updateMe(
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

    @DeleteMapping("/me")
    public void deleteMe(
            @AuthenticationPrincipal CustomUserDetails userDetailsInfo
    ) {
        userService.deleteSelf(userDetailsInfo);
    }
}
