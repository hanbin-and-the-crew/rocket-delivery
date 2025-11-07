package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.sparta.user.application.service.UserService;
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

    @Operation(summary = "hello 첫 테스트(삭제 예정)")
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Company Service!";
    }

    @Override
    @PostMapping("/signup")
    public ApiResponse<UserResponse.SignUpUser> signup(@Valid @RequestBody UserRequest.SignUpUser requestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 여러 validation 메시지를 모아서 전달
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return (ApiResponse<UserResponse.SignUpUser>) (ApiResponse<?>) ApiResponse.fail("VALIDATION_FAILED", errorMessage);
        }

        UserResponse.SignUpUser responseDto = userService.signup(requestDto);
        return ApiResponse.success(responseDto);

    }
}
