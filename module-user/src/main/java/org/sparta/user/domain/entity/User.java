package org.sparta.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.sparta.user.domain.enums.UserRoleEnum;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_name", length = 100, nullable = false, unique = true)
    private String userName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private UserRoleEnum role = UserRoleEnum.DELIVERY_PERSON;

    public User(String userName, String password, String email, String phone, UserRoleEnum role) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }
}