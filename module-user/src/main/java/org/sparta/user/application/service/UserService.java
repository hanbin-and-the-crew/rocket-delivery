package org.sparta.user.application.service;

import org.sparta.common.error.BusinessException;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.UserRoleEnum;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.infrastructure.security.CustomUserDetailsService;
import org.sparta.user.presentation.dto.request.SignUpUserRequestDto;
import org.sparta.user.presentation.dto.response.SignUpUserResponseDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final String ADMIN_TOKEN = "123";
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;

    public UserService(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         CustomUserDetailsService customUserDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customUserDetailsService = customUserDetailsService;
    }

    public String health() {
        return "ok";
    }

    @Transactional
    public SignUpUserResponseDto signup(SignUpUserRequestDto requestDto) {
        // Extract fields
        String userId = requestDto.getUserId();      // 로그인용 ID
        String userName = requestDto.getUserName();  // 사용자 이름
        String userPhone = requestDto.getUserPhone();// 휴대폰 번호
        String email = requestDto.getEmail();
        String password = passwordEncoder.encode(requestDto.getPassword());
        /*
        // 중복 체크: userId, email
        userRepository.findById(userId).ifPresent(u -> {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "중복된 사용자 ID가 존재합니다.");
        });
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,"중복된 Email 입니다.");
        });

        // ROLE 설정 (관리자 토큰 확인 시 ADMIN 부여)
        UserRoleEnum role = UserRoleEnum.CUSTOMER;
        if (requestDto.getRole().equals(UserRoleEnum.MASTER) || requestDto.getRole().equals(UserRoleEnum.MANAGER)) {
            if (!ADMIN_TOKEN.equals(requestDto.getAdminToken())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED," 잘못된 어드민 토큰입니다.");
            }
            role = requestDto.getRole();
        }*/

        UserRoleEnum role = UserRoleEnum.DELIVERY_PERSON;

        // 사용자 생성 및 저장
        User user = new User(userName, password, email, userPhone, role);
        user = userRepository.save(user);
        return toResponse(user);
    }
    private SignUpUserResponseDto toResponse(User e) {
        return new SignUpUserResponseDto(e.getId().toString(), e.getUserName());
    }
}
