package org.sparta.user.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LoginRequestDto {
    private final String userId;
    private final String password;

    @JsonCreator
    public LoginRequestDto(
            @JsonProperty("userId") String userId,
            @JsonProperty("password") String password) {
        this.userId = userId;
        this.password = password;
    }
}
