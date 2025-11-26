package org.sparta.user.presentation;

import org.sparta.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.sparta.user.application.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApiSpec {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Auth Service!";
    }

    @Override
    @PostMapping("/login")
    public ApiResponse<AuthResponse.Login> login(
            @Valid @RequestBody AuthRequest.Login request,
            HttpServletResponse LoginResponse
    ) {
        AuthResponse.Login response = authService.login(request, LoginResponse);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        authService.logout(response);
    }
}
