package org.sparta.user.presentation.dto;

import org.sparta.user.application.command.AuthCommand;
import org.sparta.user.presentation.dto.request.AuthRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthCommand.Login toCommand(AuthRequest.Login request) {
        return new AuthCommand.Login(
                request.userName(),
                request.password(),
                request.role(),
                request.authVersion()
        );
    }
}