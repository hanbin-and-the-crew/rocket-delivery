package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sparta.common.api.ApiResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User API",  description = "유저 관리" )
public interface UserApiSpec {

    @Operation(
            summary = "User 회원 가입",
            description = "필수 정보를 입력받아 유저의 회원가입을 진행합니다."
    )
    @PostMapping("/login")
    ApiResponse<UserResponse.SignUpUser> signup(
            @Valid @RequestBody UserRequest.SignUpUser requestDto,
            BindingResult bindingResult
    );



}
