package org.sparta.user.application.service;

import lombok.RequiredArgsConstructor;
import org.sparta.common.error.BusinessException;
import org.sparta.common.event.EventPublisher;
import org.sparta.user.application.command.UserCommand;
import org.sparta.user.domain.entity.User;
import org.sparta.user.domain.enums.*;
import org.sparta.user.domain.error.UserErrorType;
import org.sparta.user.domain.repository.UserRepository;
import org.sparta.user.infrastructure.event.publisher.UserCreatedEvent;
import org.sparta.user.infrastructure.event.publisher.UserDeletedEvent;
import org.sparta.user.infrastructure.event.publisher.UserRoleChangedEvent;
import org.sparta.user.infrastructure.event.publisher.UserUpdatedEvent;
import org.sparta.user.infrastructure.security.CustomUserDetails;
import org.sparta.user.infrastructure.security.CustomUserDetailsService;
import org.sparta.user.presentation.dto.request.UserRequest;
import org.sparta.user.presentation.dto.response.UserResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final EventPublisher eventPublisher; // Spring 이벤트 퍼블리셔


    public String health() {
        return "ok";
    }

    /**
     * POST users/signup
     */
    @Transactional
    @CacheEvict(value = "userListCache", allEntries = true)
    public UserResponse.SignUpUser signup(UserCommand.SignUpUser request) {

        String userName = request.userName();
        String realName = request.realName();
        String userPhoneNumber = request.userPhoneNumber();
        String email = request.email();
        String password = passwordEncoder.encode(request.password());
        String slackId = request.slackId();
        UUID hubId = request.hubId();
        UserRoleEnum role = request.role();
        DeliveryManagerRoleEnum deliveryManagerRoleEnum = request.deliveryRole();

        // 중복 체크: userName, email
        userRepository.findByUserName(userName).ifPresent(u -> {
            throw new BusinessException(UserErrorType.VALIDATION_FAILED, "중복된 사용자 ID가 존재합니다.");
        });
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new BusinessException(UserErrorType.VALIDATION_FAILED,"중복된 Email 입니다.");
        });

        // 사용자 생성 및 저장
        User user = User.create(
                userName, password, slackId, realName,
                userPhoneNumber, email, role, hubId, deliveryManagerRoleEnum);

        user = userRepository.save(user);

        // 유저 생성 이벤트 발행
        eventPublisher.publishExternal(UserCreatedEvent.of(user));

        return UserResponse.SignUpUser.from(user);
    }

    /**
     * GET /users/me
     */
    public UserResponse.GetUser getUserInfo(CustomUserDetails userDetailsInfo) {
        User user = userRepository.findByUserId(userDetailsInfo.getId())
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND, "회원이 존재하지 않습니다."));
        return UserResponse.GetUser.from(user);
    }

    /**
     * PATCH /users/me
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#user.id"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public UserResponse.UpdateUser updateSelf(CustomUserDetails user, UserCommand.UpdateUser request) {
        User userInfo = userRepository.findByUserId(user.getId()).orElseThrow(
                () -> new BusinessException(UserErrorType.UNAUTHORIZED,"수정할 유저 정보가 없습니다.")
        );
        if (userInfo.getDeletedAt() != null) {
            throw new BusinessException(UserErrorType.UNAUTHORIZED," 탈퇴한 회원입니다.");
        }
        String newPassword = passwordEncoder.encode(request.newPassword().trim());

        // JPA Dirty Checking으로 업데이트: 새 엔티티 생성/저장은 금지
        if (request.userName() != null && !request.userName().isBlank()) {
            userInfo.updateUserName(request.userName().trim());
        }
        // 비밀번호
        if (passwordEncoder.encode(request.oldPassword()).equals(user.getPassword()) && request.newPassword() != null && !request.newPassword().isBlank()) {
            userInfo.updatePassword(newPassword);
        }
        // 이메일
        if (request.email() != null && !request.email().isBlank()) {
            userInfo.updateEmail(request.email().trim());
        }
        //전화번호
        if (request.userPhoneNumber() != null && !request.userPhoneNumber().isBlank()) {
            userInfo.updatePhoneNumber(request.userPhoneNumber().trim());
        }

        User updateUser = userRepository.save(userInfo);

        /*
        정보수정에 오류가 발생하여 기존 SpringSecurity에 저장된 SecurityContext 또한 수정을 해야한다는 것을 알게되어
        기존 정보 호출
         */
        CustomUserDetails updatedUserDetails = customUserDetailsService.loadUserByUsername(updateUser.getUserName());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        /*
        호출된 값을 setAuthentication를 사용하여 새 값으로 변경하고 최종 setContext하여 업데이트 처리
         */

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(updateAuthentication(authentication, updatedUserDetails));
        SecurityContextHolder.setContext(context);

        // 유저 정보 변경 이벤트 발행
        eventPublisher.publishExternal(UserUpdatedEvent.of(updateUser));

        return UserResponse.UpdateUser.from(updateUser);
    }

    // 업데이트 인증처리에 대한 추가적인 메서드를 작성하여 기존 인증값과 현재 업데이트 된 값을 가져와서 업데이트 처리
    protected Authentication updateAuthentication(Authentication authentication, CustomUserDetails userDetails) {
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                userDetails, authentication.getCredentials(), userDetails.getAuthorities());
        newAuth.setDetails(authentication.getDetails());

        return newAuth;
    }

    /**
     * DELETE /User/{userId}
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#user.Id"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public void deleteSelf(CustomUserDetails userDetails) {
        User user = userRepository.findByUserId(userDetails.getId())
                .orElseThrow(() -> new BusinessException(UserErrorType.NOT_FOUND, "이미 탈퇴했거나 존재하지 않는 회원입니다."));

        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        int updated = userRepository.softDeleteByUserId(user.getUserId(), now);
        if (updated == 0) {
            throw new BusinessException(UserErrorType.NOT_FOUND, "이미 탈퇴했거나 존재하지 않는 회원입니다.");
        }

        // 유저 삭제 이벤트 발행
        eventPublisher.publishExternal(UserDeletedEvent.of(user));
    }

    /**
     * POST /User/id-find
     */
    @Transactional
    public UserResponse.FindUserId findUserId(UserCommand.FindUserId request) {
        String email = request.email();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException(UserErrorType.USER_NOT_FOUND, "해당 이메일로 가입된 사용자가 없습니다.")
                );

        return UserResponse.FindUserId.from(user);
    }

    /**
     * GET /users/bos/{userId}
     */
    @Cacheable(value = "userCache", key = "#userId")
    public UserResponse.GetUser getSpecificUserInfo(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND, "회원이 존재하지 않습니다."));
        return UserResponse.GetUser.from(user);
    }

    /**
     * PATCH /users/me
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#userid"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public UserResponse.UpdateUser updateUser(UUID userId, UserCommand.UpdateUser request) {
        User userInfo = userRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException(UserErrorType.UNAUTHORIZED,"수정할 유저 정보가 없습니다.")
        );
        if (userInfo.getDeletedAt() != null) {
            throw new BusinessException(UserErrorType.UNAUTHORIZED," 탈퇴한 회원입니다.");
        }
        String newPassword = passwordEncoder.encode(request.newPassword().trim());

        // JPA Dirty Checking으로 업데이트: 새 엔티티 생성/저장은 금지
        if (request.userName() != null && !request.userName().isBlank()) {
            userInfo.updateUserName(request.userName().trim());
        }
        // 비밀번호
        if (passwordEncoder.encode(request.oldPassword()).equals(userInfo.getPassword()) && request.newPassword() != null && !request.newPassword().isBlank()) {
            userInfo.updatePassword(newPassword);
        }
        // 이메일
        if (request.email() != null && !request.email().isBlank()) {
            userInfo.updateEmail(request.email().trim());
        }
        //전화번호
        if (request.userPhoneNumber() != null && !request.userPhoneNumber().isBlank()) {
            userInfo.updatePhoneNumber(request.userPhoneNumber().trim());
        }

        User updateUser = userRepository.save(userInfo);

        /*
        정보수정에 오류가 발생하여 기존 SpringSecurity에 저장된 SecurityContext 또한 수정을 해야한다는 것을 알게되어
        기존 정보 호출
         */
        CustomUserDetails updatedUserDetails = customUserDetailsService.loadUserByUsername(updateUser.getUserName());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        /*
        호출된 값을 setAuthentication를 사용하여 새 값으로 변경하고 최종 setContext하여 업데이트 처리
         */

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(updateAuthentication(authentication, updatedUserDetails));
        SecurityContextHolder.setContext(context);

        // 유저 정보 변경 이벤트 발행
        eventPublisher.publishExternal(UserUpdatedEvent.of(updateUser));

        return UserResponse.UpdateUser.from(updateUser);
    }

    /**
     * DELETE /users/bos/{userId}
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userCache", key = "#userId"),
            @CacheEvict(value = "userListCache", allEntries = true)
    })
    public void deleteUser(UUID userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.NOT_FOUND, "이미 탈퇴했거나 존재하지 않는 회원입니다."));

        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        int updated = userRepository.softDeleteByUserId(user.getUserId(), now);
        if (updated == 0) {
            throw new BusinessException(UserErrorType.NOT_FOUND, "이미 탈퇴했거나 존재하지 않는 회원입니다.");
        }

        // 유저 삭제 이벤트 발행
        eventPublisher.publishExternal(UserDeletedEvent.of(user));
    }

    /**
     * GET /users/bos
     */
    @Cacheable(value = "userListCache", key = "'allUsers'")
    public List<UserResponse.GetUser> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserResponse.GetUser::from)
                .toList();
    }

    /**
     * POST /users/bos/{userId}/APPROVE
     * POST /users/bos/{userId}/REJECTED
     */
    @Transactional
    @CacheEvict(value = "userListCache", allEntries = true)
    public void updateUserStatus(UUID userId, UserStatusEnum newStatus) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserErrorType.USER_NOT_FOUND, "회원이 존재하지 않습니다."));

        if (user.getStatus() != UserStatusEnum.PENDING) {
            throw new BusinessException(UserErrorType.INVALID_STATUS_CHANGE, "대기중인 회원만 상태를 변경할 수 있습니다.");
        }

        user.updateStatus(newStatus);

        // 유저 권한 변경 이벤트 발행
        eventPublisher.publishExternal(UserRoleChangedEvent.of(user));
    }


}
