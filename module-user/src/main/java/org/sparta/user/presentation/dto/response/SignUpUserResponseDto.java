package org.sparta.user.presentation.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class SignUpUserResponseDto {

    private String UserId;
    private String UserName;

    public SignUpUserResponseDto(String UserId, String UserName) {
        this.UserId = UserId;
        this.UserName = UserName;
    }

    public static SignUpUserResponseDto of(String UserId, String UserName) {     // 객체를 만들어낸다는 의미로 관용적으로 쓰이는 이름
        return new SignUpUserResponseDto(UserId, UserName);
    }
}
