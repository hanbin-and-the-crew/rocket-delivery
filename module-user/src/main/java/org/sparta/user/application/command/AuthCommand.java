package org.sparta.user.application.command;

import org.sparta.user.domain.enums.UserRoleEnum;

public class AuthCommand {

    public record Login(
            String userName,
            String password,
            UserRoleEnum role,
            Integer authVersion
    ) {}
}