package org.sparta.user.presentation.Controller;

import org.sparta.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.sparta.user.application.command.AuthCommand;
import org.sparta.user.application.service.AuthService;
import org.sparta.user.presentation.ApiSpec.AuthApiSpec;
import org.sparta.user.presentation.dto.AuthMapper;
import org.sparta.user.presentation.dto.request.AuthRequest;
import org.sparta.user.presentation.dto.response.AuthResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApiSpec {

    private final AuthService authService;

    private final AuthMapper authMapper;

    public AuthController(AuthService authService, AuthMapper authMapper) {
        this.authService = authService;
        this.authMapper = authMapper;
    }

    @GetMapping("/health")
    public String hello() {
        return "Auth OK";
    }

    @Override
    @PostMapping("/login")
    public ApiResponse<AuthResponse.Login> login(
            @Valid @RequestBody AuthRequest.Login request,
            HttpServletResponse LoginResponse
    ) {
        AuthCommand.Login command = authMapper.toCommand(request);
        AuthResponse.Login response = authService.login(command, LoginResponse);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        authService.logout(response);
    }
}
