package org.sparta.user.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.application.service.UserService;
import org.sparta.user.presentation.dto.request.SignUpUserRequestDto;
import org.sparta.user.presentation.dto.response.SignUpUserResponseDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "hello 첫 테스트(삭제 예정)")
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Company Service!";
    }

    @Operation(summary = "User 회원 가입")
    @PostMapping("/signup")
    public ApiResponse<Object> signup(@Valid @RequestBody SignUpUserRequestDto requestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 여러 validation 메시지를 모아서 전달
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ApiResponse.fail("VALIDATION_FAILED", errorMessage);
        }

        SignUpUserResponseDto responseDto = userService.signup(requestDto);
        return ApiResponse.success(responseDto);

    }
}
