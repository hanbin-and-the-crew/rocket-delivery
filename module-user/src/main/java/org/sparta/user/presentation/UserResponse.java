package org.sparta.user.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

public class UserResponse {

    @Schema(description = "회원가입 응답")
    public record SignUpUser(
            @Schema(description = "사용자 이름", example = "user1107")
            String userName
    ) {
    }


}
